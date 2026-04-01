package com.urlshortener.controller;

import java.net.URI;

import com.urlshortener.service.UrlShortenerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @GetMapping("/{shortCode:[0-9a-zA-Z]{4,24}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, jakarta.servlet.http.HttpServletRequest request) {
        String originalUrl = urlShortenerService.resolveOriginalUrl(shortCode);
        
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");
        String ipAddress = request.getRemoteAddr();
        
        urlShortenerService.recordRedirect(shortCode, userAgent, referrer, ipAddress);

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }
}
