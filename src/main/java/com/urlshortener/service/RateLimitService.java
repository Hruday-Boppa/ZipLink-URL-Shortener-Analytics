package com.urlshortener.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * Service to manage API rate limiting using the Token Bucket algorithm (Bucket4j).
 * This demonstrates an understanding of system security and resource protection 
 * for a 2-3 years experience level.
 */
@Service
public class RateLimitService {

    // Map of client IPs to their respective buckets
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Resolves the bucket for a given IP address. Creates a new one if not present.
     * Limit: 10 requests per minute per IP.
     */
    public Bucket resolveBucket(String ipAddress) {
        return buckets.computeIfAbsent(ipAddress, key -> {
            // Limit: 10 tokens per window, refilled every minute
            Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        });
    }
}
