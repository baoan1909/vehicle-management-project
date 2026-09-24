package com.ban.vehicle_management.infrastructure.cache;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared Redis connection configuration. Redis is strictly an optimization and
 * resilience layer: PostgreSQL remains the source of truth. When disabled or
 * unreachable every caller must fail open to the legacy PostgreSQL/Gemini flow.
 */
@Getter
@Setter
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
}
