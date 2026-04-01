package com.urlshortener.dto;

import java.time.Instant;

public record ErrorResponse(
    String error,
    String message,
    int status,
    Instant timestamp
) {
}
