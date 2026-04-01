package com.urlshortener.dto;

import java.util.List;

public record DetailedAnalyticsResponse(
    List<DailyClickStatsResponse> dailyStats,
    List<BrowserStatsResponse> browserStats,
    List<OSStatsResponse> osStats
) {
}
