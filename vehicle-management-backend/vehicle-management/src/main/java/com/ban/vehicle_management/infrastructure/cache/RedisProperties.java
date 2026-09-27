package com.ban.vehicle_management.infrastructure.cache;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Shared Redis connection configuration. Redis is strictly an optimization and
 * resilience layer: PostgreSQL remains the source of truth. When disabled or
 * unreachable every caller must fail open to the legacy PostgreSQL/Gemini flow.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {

    private boolean enabled = true;
    private String host = "localhost";
    private int port = 6379;
    private String username = "";
    private String password = "";
    private boolean sslEnabled = false;
    private Duration connectTimeout = Duration.ofSeconds(1);
    private Duration commandTimeout = Duration.ofSeconds(1);
    private String environment = "local";
    private String service = "vehicle-management";

    @AssertTrue(message = "Redis connect/command timeouts must be greater than zero")
    public boolean areTimeoutsValid() {
        return isPositive(connectTimeout) && isPositive(commandTimeout);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
