package com.urlshortener.service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.urlshortener.config.AppProperties;
import org.springframework.stereotype.Component;

@Component
public class CustomHashShortCodeGenerator {

    private final AtomicInteger rollingSequence = new AtomicInteger(0);
    private final HashingService hashingService;
    private final AppProperties appProperties;

    public CustomHashShortCodeGenerator(HashingService hashingService, AppProperties appProperties) {
        this.hashingService = hashingService;
        this.appProperties = appProperties;
    }

    public String generateCode() {
        int sequence = rollingSequence.updateAndGet(current -> (current + 1) & 4095);
        long timestampMs = Instant.now().toEpochMilli();
        long entropy = ThreadLocalRandom.current().nextLong();

        String payload = appProperties.getShortCode().getSecret() + ":" + timestampMs + ":" + sequence + ":" + entropy;
        byte[] digest = hashingService.sha256Bytes(payload);
        return Base62Encoder.toFixedLength(digest, appProperties.getShortCode().getLength());
    }
}
