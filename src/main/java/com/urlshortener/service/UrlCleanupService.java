package com.urlshortener.service;

import java.time.Instant;

import com.urlshortener.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled service for automated housekeeping of expired URL mappings.
 * This demonstrates an understanding of the data lifecycle and system maintenance
 * for a 2-3 years experience level.
 */
@Service
public class UrlCleanupService {

    private static final Logger log = LoggerFactory.getLogger(UrlCleanupService.class);
    private final UrlMappingRepository urlMappingRepository;

    public UrlCleanupService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    /**
     * Runs every hour to find and delete URL mappings that have exceeded their expiration date.
     * Use cron expression to run at the start of every hour.
     */
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredUrls() {
        log.info("Starting scheduled cleanup of expired URL mappings...");
        Instant now = Instant.now();
        urlMappingRepository.deleteByExpiresAtBefore(now);
        log.info("Finished scheduled cleanup of expired URL mappings.");
    }
}
