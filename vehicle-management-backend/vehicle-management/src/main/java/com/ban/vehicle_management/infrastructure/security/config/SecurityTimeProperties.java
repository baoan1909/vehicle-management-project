package com.ban.vehicle_management.infrastructure.security.config;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.oauth2.jwk")
public class SecurityTimeProperties {

    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(15);

    @AssertTrue(message = "JWK timeouts must be greater than zero")
    public boolean areTimeoutsValid() {
        return isPositive(connectTimeout) && isPositive(readTimeout);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
