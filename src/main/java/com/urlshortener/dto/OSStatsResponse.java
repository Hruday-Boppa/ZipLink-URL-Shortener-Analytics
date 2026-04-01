package com.urlshortener.dto;

public record OSStatsResponse(
    String os,
    long clicks
) {
}
