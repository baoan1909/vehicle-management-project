package com.ban.vehicle_management.infrastructure.security.ratelimit;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.maps.rate-limit")
public class MapApiRateLimitProperties {

    private boolean enabled = true;
    private Duration window = Duration.ofMinutes(1);
    private int anonymousIpLimit = 30;
    private int authenticatedIpLimit = 120;
    private int accountLimit = 120;

    @AssertTrue(message = "Map API rate-limit configuration is invalid")
    public boolean isValid() {
        return window != null && !window.isZero() && !window.isNegative()
                && window.compareTo(Duration.ofHours(1)) <= 0
                && anonymousIpLimit >= 1
                && authenticatedIpLimit >= anonymousIpLimit
                && accountLimit >= 1;
    }
}
