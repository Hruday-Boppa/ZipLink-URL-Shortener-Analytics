package com.urlshortener.repository;

import java.util.List;

import com.urlshortener.domain.ClickAnalytics;
import com.urlshortener.dto.BrowserStatsResponse;
import com.urlshortener.dto.DailyClickStatsResponse;
import com.urlshortener.dto.OSStatsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    /**
     * Aggregates click counts grouped by date for the given short code.
     * Uses CAST to extract just the date portion from the clickedAt timestamp.
     */
    @Query("""
        SELECT new com.urlshortener.dto.DailyClickStatsResponse(
            CAST(c.clickedAt AS date),
            COUNT(c)
        )
        FROM ClickAnalytics c
        WHERE c.shortCode = :shortCode
        GROUP BY CAST(c.clickedAt AS date)
        ORDER BY CAST(c.clickedAt AS date) DESC
    """)
    List<DailyClickStatsResponse> findDailyClickStats(@Param("shortCode") String shortCode);

    @Query("""
        SELECT new com.urlshortener.dto.BrowserStatsResponse(
            c.browser,
            COUNT(c)
        )
        FROM ClickAnalytics c
        WHERE c.shortCode = :shortCode AND c.browser IS NOT NULL
        GROUP BY c.browser
        ORDER BY COUNT(c) DESC
    """)
    List<BrowserStatsResponse> findBrowserStats(@Param("shortCode") String shortCode);

    @Query("""
        SELECT new com.urlshortener.dto.OSStatsResponse(
            c.os,
            COUNT(c)
        )
        FROM ClickAnalytics c
        WHERE c.shortCode = :shortCode AND c.os IS NOT NULL
        GROUP BY c.os
        ORDER BY COUNT(c) DESC
    """)
    List<OSStatsResponse> findOSStats(@Param("shortCode") String shortCode);
}

