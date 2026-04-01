package com.urlshortener.dto;

public record BrowserStatsResponse(
    String browser,
    long clicks
) {
}
