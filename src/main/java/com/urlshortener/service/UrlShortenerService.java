package com.urlshortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.urlshortener.config.AppProperties;
import com.urlshortener.domain.UrlMapping;
import com.urlshortener.dto.BrowserStatsResponse;
import com.urlshortener.dto.CreateShortUrlRequest;
import com.urlshortener.dto.CreateShortUrlResponse;
import com.urlshortener.dto.DailyClickStatsResponse;
import com.urlshortener.dto.DetailedAnalyticsResponse;
import com.urlshortener.dto.OSStatsResponse;
import com.urlshortener.dto.UrlStatsResponse;
import com.urlshortener.exception.ExpiredUrlException;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.UrlMappingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final StringRedisTemplate redisTemplate;
    private final HashingService hashingService;
    private final CustomHashShortCodeGenerator shortCodeGenerator;
    private final AppProperties appProperties;
    private final ClickTrackingService clickTrackingService;
    private final QrCodeService qrCodeService;
    private final ClickAnalyticsRepository clickAnalyticsRepository;

    public UrlShortenerService(
        UrlMappingRepository urlMappingRepository,
        StringRedisTemplate redisTemplate,
        HashingService hashingService,
        CustomHashShortCodeGenerator shortCodeGenerator,
        AppProperties appProperties,
        ClickTrackingService clickTrackingService,
        QrCodeService qrCodeService,
        ClickAnalyticsRepository clickAnalyticsRepository
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.redisTemplate = redisTemplate;
        this.hashingService = hashingService;
        this.shortCodeGenerator = shortCodeGenerator;
        this.appProperties = appProperties;
        this.clickTrackingService = clickTrackingService;
        this.qrCodeService = qrCodeService;
        this.clickAnalyticsRepository = clickAnalyticsRepository;
    }

    @Transactional
    public CreateShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        String normalizedUrl = normalizeUrl(request.originalUrl());
        Instant expiresAt = validateExpiry(request.expiresAt());
        Instant now = Instant.now();
        String originalUrlHash = hashingService.sha256Hex(normalizedUrl);

        if (request.alias() != null && !request.alias().isBlank()) {
            validateShortCode(request.alias());
            if (urlMappingRepository.findByShortCode(request.alias()).isPresent()) {
                throw new InvalidUrlException("Alias is already taken");
            }
            UrlMapping mapping = new UrlMapping();
            mapping.setOriginalUrl(normalizedUrl);
            mapping.setOriginalUrlHash(originalUrlHash);
            mapping.setShortCode(request.alias());
            mapping.setExpiresAt(expiresAt);
            mapping.setOneTimeUse(request.oneTimeUse());

            try {
                UrlMapping saved = urlMappingRepository.saveAndFlush(mapping);
                cacheMapping(saved);
                return toCreateResponse(saved, false);
            } catch (DataIntegrityViolationException ex) {
                throw new InvalidUrlException("Alias is already taken");
            }
        }

        Optional<UrlMapping> existingMapping = urlMappingRepository.findTopByOriginalUrlHashOrderByCreatedAtDesc(originalUrlHash)
            .filter(mapping -> mapping.getOriginalUrl().equals(normalizedUrl))
            .filter(mapping -> !mapping.isExpired(now));
        if (existingMapping.isPresent()) {
            cacheMapping(existingMapping.get());
            return toCreateResponse(existingMapping.get(), true);
        }

        int maxAttempts = appProperties.getShortCode().getMaxAttempts();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String code = shortCodeGenerator.generateCode();
            UrlMapping mapping = new UrlMapping();
            mapping.setOriginalUrl(normalizedUrl);
            mapping.setOriginalUrlHash(originalUrlHash);
            mapping.setShortCode(code);
            mapping.setExpiresAt(expiresAt);
            mapping.setOneTimeUse(request.oneTimeUse());

            try {
                UrlMapping saved = urlMappingRepository.saveAndFlush(mapping);
                cacheMapping(saved);
                return toCreateResponse(saved, false);
            } catch (DataIntegrityViolationException ex) {
                // Retry with another generated code when a unique key collision occurs.
            }
        }

        throw new IllegalStateException("Unable to generate a unique short code");
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getUrlStats(String shortCode) {
        validateShortCode(shortCode);
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));

        String qrCodeBase64 = qrCodeService.generateQrCodeBase64(buildPublicShortUrl(mapping.getShortCode()), 250, 250);
        
        return new UrlStatsResponse(
            mapping.getShortCode(),
            buildPublicShortUrl(mapping.getShortCode()),
            mapping.getOriginalUrl(),
            qrCodeBase64,
            mapping.getClickCount(),
            mapping.getCreatedAt(),
            mapping.getExpiresAt(),
            mapping.isExpired(Instant.now())
        );
    }

    @Transactional
    public String resolveOriginalUrl(String shortCode) {
        validateShortCode(shortCode);

        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));

        if (mapping.isExpired(Instant.now())) {
            safeDeleteCacheKey(shortCode);
            throw new ExpiredUrlException("Short URL has expired");
        }

        String originalUrl = mapping.getOriginalUrl();

        // Burn after read: delete the mapping immediately after resolving
        if (mapping.isOneTimeUse()) {
            urlMappingRepository.delete(mapping);
            safeDeleteCacheKey(shortCode);
            return originalUrl;
        }

        // For normal links, try cache first
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey(shortCode));
            if (cached != null) {
                return cached;
            }
        } catch (Exception ex) {
            // Fall back to DB value if Redis is unavailable.
        }

        cacheMapping(mapping);
        return originalUrl;
    }

    public void recordRedirect(String shortCode, String userAgent, String referrer, String ipAddress) {
        clickTrackingService.recordClick(shortCode, userAgent, referrer, ipAddress);
    }

    @Transactional(readOnly = true)
    public List<DailyClickStatsResponse> getDailyClickStats(String shortCode) {
        validateShortCode(shortCode);
        if (!urlMappingRepository.findByShortCode(shortCode).isPresent()) {
            throw new ResourceNotFoundException("Short URL not found");
        }
        return clickAnalyticsRepository.findDailyClickStats(shortCode);
    }

    @Transactional(readOnly = true)
    public DetailedAnalyticsResponse getDetailedAnalytics(String shortCode) {
        validateShortCode(shortCode);
        if (!urlMappingRepository.findByShortCode(shortCode).isPresent()) {
            throw new ResourceNotFoundException("Short URL not found");
        }

        List<DailyClickStatsResponse> dailyStats = clickAnalyticsRepository.findDailyClickStats(shortCode);
        List<BrowserStatsResponse> browserStats = clickAnalyticsRepository.findBrowserStats(shortCode);
        List<OSStatsResponse> osStats = clickAnalyticsRepository.findOSStats(shortCode);

        return new DetailedAnalyticsResponse(dailyStats, browserStats, osStats);
    }

    private CreateShortUrlResponse toCreateResponse(UrlMapping mapping, boolean reused) {
        String qrCodeBase64 = qrCodeService.generateQrCodeBase64(buildPublicShortUrl(mapping.getShortCode()), 250, 250);
        return new CreateShortUrlResponse(
            mapping.getShortCode(),
            buildPublicShortUrl(mapping.getShortCode()),
            mapping.getOriginalUrl(),
            qrCodeBase64,
            mapping.getCreatedAt(),
            mapping.getExpiresAt(),
            reused
        );
    }

    private String buildPublicShortUrl(String shortCode) {
        String baseUrl = appProperties.getBaseUrl();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + "/" + shortCode;
    }

    private void cacheMapping(UrlMapping mapping) {
        Duration ttl = appProperties.getCache().getDefaultTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = Duration.ofHours(24);
        }
        if (mapping.getExpiresAt() != null) {
            Duration remaining = Duration.between(Instant.now(), mapping.getExpiresAt());
            if (remaining.isNegative() || remaining.isZero()) {
                safeDeleteCacheKey(mapping.getShortCode());
                return;
            }
            ttl = remaining.compareTo(ttl) < 0 ? remaining : ttl;
        }

        try {
            redisTemplate.opsForValue().set(cacheKey(mapping.getShortCode()), mapping.getOriginalUrl(), ttl);
        } catch (Exception ex) {
            // Ignore cache write failures so request path remains available.
        }
    }

    private Instant validateExpiry(Instant expiresAt) {
        if (expiresAt == null) {
            return null;
        }
        if (!expiresAt.isAfter(Instant.now())) {
            throw new InvalidUrlException("expiresAt must be a future timestamp");
        }
        return expiresAt;
    }

    private String normalizeUrl(String rawUrl) {
        String value = rawUrl == null ? "" : rawUrl.trim();
        try {
            URI parsed = new URI(value);
            String scheme = parsed.getScheme();
            String host = parsed.getHost();
            if (scheme == null || host == null) {
                throw new InvalidUrlException("URL must include scheme and host");
            }

            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
                throw new InvalidUrlException("URL scheme must be http or https");
            }

            URI normalized = new URI(
                normalizedScheme,
                parsed.getRawUserInfo(),
                host.toLowerCase(Locale.ROOT),
                parsed.getPort(),
                parsed.getRawPath(),
                parsed.getRawQuery(),
                null
            );

            String normalizedUrl = normalized.toString();
            if (normalizedUrl.length() > 2048) {
                throw new InvalidUrlException("URL length must be <= 2048 characters");
            }
            return normalizedUrl;
        } catch (URISyntaxException ex) {
            throw new InvalidUrlException("Invalid URL format");
        }
    }

    private void validateShortCode(String shortCode) {
        if (shortCode == null || !shortCode.matches("^[0-9a-zA-Z]{4,24}$")) {
            throw new InvalidUrlException("Invalid short code format");
        }
    }

    private String cacheKey(String shortCode) {
        return "url:code:" + shortCode;
    }

    private void safeDeleteCacheKey(String shortCode) {
        try {
            redisTemplate.delete(cacheKey(shortCode));
        } catch (Exception ex) {
            // Ignore cache delete failures.
        }
    }
}
