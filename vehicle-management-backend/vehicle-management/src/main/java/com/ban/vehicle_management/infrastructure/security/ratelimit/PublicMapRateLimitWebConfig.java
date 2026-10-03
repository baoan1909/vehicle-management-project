package com.ban.vehicle_management.infrastructure.security.ratelimit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PublicMapRateLimitWebConfig implements WebMvcConfigurer {

    private final PublicMapRateLimitInterceptor interceptor;

    public PublicMapRateLimitWebConfig(PublicMapRateLimitInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns(
                        "/api/public/parking-lots/nearby",
                        "/api/public/parking-lots/telemetry/**",
                        "/api/public/parking-locations/**"
                );
    }
}
