package com.urlshortener.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
    @NotBlank(message = "originalUrl is required")
    @Size(max = 2048, message = "originalUrl must be <= 2048 characters")
    String originalUrl,

    @Size(min = 4, max = 24, message = "alias must be between 4 and 24 characters")
    String alias,

    Instant expiresAt,

    // If true, the link is deleted after the first click (burn after read)
    boolean oneTimeUse
) {
}
