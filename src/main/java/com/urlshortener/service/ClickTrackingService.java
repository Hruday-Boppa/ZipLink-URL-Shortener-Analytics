package com.urlshortener.service;

import java.time.Instant;

import com.urlshortener.domain.ClickAnalytics;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.UrlMappingRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua_parser.Client;
import ua_parser.Parser;

@Service
public class ClickTrackingService {
    private static final Parser uaParser = new Parser();

    private final UrlMappingRepository urlMappingRepository;
    private final ClickAnalyticsRepository clickAnalyticsRepository;

    public ClickTrackingService(UrlMappingRepository urlMappingRepository, ClickAnalyticsRepository clickAnalyticsRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.clickAnalyticsRepository = clickAnalyticsRepository;
    }

    @Async
    @Transactional
    public void recordClick(String shortCode, String userAgent, String referrer, String ipAddress) {
        urlMappingRepository.incrementClickCount(shortCode, Instant.now());
        
        ClickAnalytics analytics = new ClickAnalytics();
        analytics.setShortCode(shortCode);
        analytics.setUserAgent(userAgent);
        analytics.setReferrer(referrer);
        analytics.setIpAddress(ipAddress);

        if (userAgent != null) {
            Client client = uaParser.parse(userAgent);
            analytics.setBrowser(client.userAgent.family);
            analytics.setOs(client.os.family);
        }

        clickAnalyticsRepository.save(analytics);
    }
}
