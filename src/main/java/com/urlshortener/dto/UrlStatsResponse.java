package com.urlshortener.dto;

import java.time.Instant;

public record UrlStatsResponse(
    String shortCode,
    String shortUrl,
    String originalUrl,
    String qrCodeBase64,
    long clickCount,
    Instant createdAt,
    Instant expiresAt,
    boolean expired
) {
}
