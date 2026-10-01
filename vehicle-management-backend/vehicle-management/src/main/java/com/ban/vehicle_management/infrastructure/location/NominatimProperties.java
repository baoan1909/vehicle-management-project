package com.ban.vehicle_management.infrastructure.location;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.geocoding.nominatim")
public class NominatimProperties {
    private String baseUrl = "https://nominatim.openstreetmap.org";
    private Set<String> allowedHosts = new LinkedHashSet<>(Set.of("nominatim.openstreetmap.org"));
    private String userAgent = "CoParking/1.0 (contact: dev@coparking.local)";
    private String acceptLanguage = "vi";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);
    private Duration cacheTtl = Duration.ofDays(7);
    private int maxAttempts = 2;
    private int requestsPerSecond = 1;
    private int dailyRequestLimit = 1_000;

    @AssertTrue(message = "Nominatim configuration is invalid")
    public boolean isValid() {
        return baseUrl != null && !baseUrl.isBlank()
                && allowedHosts != null && !allowedHosts.isEmpty()
                && userAgent != null && userAgent.contains("contact:")
                && connectTimeout != null && !connectTimeout.isNegative() && !connectTimeout.isZero()
                && readTimeout != null && !readTimeout.isNegative() && !readTimeout.isZero()
                && cacheTtl != null && !cacheTtl.isNegative() && !cacheTtl.isZero()
                && maxAttempts >= 1 && maxAttempts <= 3
                && requestsPerSecond == 1
                && dailyRequestLimit >= 1;
    }
}
