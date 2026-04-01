package com.urlshortener.dto;

import java.time.LocalDate;

public record DailyClickStatsResponse(
    String date,   // e.g. "2026-03-30"
    long clicks
) {

    public DailyClickStatsResponse(LocalDate date, Long clicks) {
        this(date != null ? date.toString() : null, clicks == null ? 0L : clicks);
    }

    public DailyClickStatsResponse(java.sql.Date date, Long clicks) {
        this(date != null ? date.toLocalDate().toString() : null, clicks == null ? 0L : clicks);
    }
}
