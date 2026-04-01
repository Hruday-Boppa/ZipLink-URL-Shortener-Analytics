package com.urlshortener.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Modern Web Configuration to register the Rate Limiting interceptor.
 * This ensures that every API request for critical endpoints is filtered.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @NonNull
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(@NonNull RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Apply rate limiting to all v1 API endpoints
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/urls/**");
    }
}
