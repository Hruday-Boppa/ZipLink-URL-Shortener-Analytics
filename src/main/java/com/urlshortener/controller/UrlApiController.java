package com.urlshortener.controller;

import java.util.List;

import com.urlshortener.dto.CreateShortUrlRequest;
import com.urlshortener.dto.CreateShortUrlResponse;
import com.urlshortener.dto.DailyClickStatsResponse;
import com.urlshortener.dto.DetailedAnalyticsResponse;
import com.urlshortener.dto.UrlStatsResponse;
import com.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlApiController {

    private final UrlShortenerService urlShortenerService;

    public UrlApiController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request) {
        CreateShortUrlResponse response = urlShortenerService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}")
    public UrlStatsResponse getUrlStats(@PathVariable String shortCode) {
        return urlShortenerService.getUrlStats(shortCode);
    }

    @GetMapping("/{shortCode}/analytics")
    public List<DailyClickStatsResponse> getDailyAnalytics(@PathVariable String shortCode) {
        return urlShortenerService.getDailyClickStats(shortCode);
    }

    @GetMapping("/{shortCode}/analytics/details")
    public DetailedAnalyticsResponse getDetailedAnalytics(@PathVariable String shortCode) {
        return urlShortenerService.getDetailedAnalytics(shortCode);
    }
}

