package com.ban.vehicle_management.application.iam.account.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@ConfigurationProperties(prefix = "app.iam.verification-email")
public class VerificationTimeProperties {

    @NotNull
    private Duration resendCooldown = Duration.ofMinutes(1);

    @NotNull
    private Duration rateWindow = Duration.ofHours(1);

    @Positive
    private int maxRequests = 5;

    @AssertTrue(message = "Verification email durations must be greater than zero")
    public boolean areDurationsValid() {
        return isPositive(resendCooldown) && isPositive(rateWindow);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
