package com.urlshortener.service;

import java.time.Instant;
import java.util.List;

import com.urlshortener.config.AppProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private static final DefaultRedisScript<Long> INCREMENT_AND_EXPIRE_SCRIPT;

    static {
        INCREMENT_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        INCREMENT_AND_EXPIRE_SCRIPT.setResultType(Long.class);
        INCREMENT_AND_EXPIRE_SCRIPT.setScriptText("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
        """);
    }

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public RateLimiterService(StringRedisTemplate redisTemplate, AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
    }

    public RateLimitDecision consume(String key) {
        long windowSeconds = Math.max(1, appProperties.getRateLimit().getWindow().toSeconds());
        long currentEpochSeconds = Instant.now().getEpochSecond();
        long windowBucket = currentEpochSeconds / windowSeconds;
        long windowResetEpochSeconds = (windowBucket + 1) * windowSeconds;

        String redisKey = "rate_limit:" + key + ":" + windowBucket;
        long currentCount;
        try {
            Long count = redisTemplate.execute(
                INCREMENT_AND_EXPIRE_SCRIPT,
                List.of(redisKey),
                String.valueOf(windowSeconds)
            );
            currentCount = count == null ? appProperties.getRateLimit().getRequestsPerMinute() + 1L : count;
        } catch (Exception ex) {
            return new RateLimitDecision(true, appProperties.getRateLimit().getRequestsPerMinute(), windowResetEpochSeconds);
        }

        int limit = appProperties.getRateLimit().getRequestsPerMinute();
        boolean allowed = currentCount <= limit;
        long remaining = Math.max(0, limit - currentCount);
        return new RateLimitDecision(allowed, remaining, windowResetEpochSeconds);
    }

    public record RateLimitDecision(boolean allowed, long remaining, long resetEpochSeconds) {
    }
}
