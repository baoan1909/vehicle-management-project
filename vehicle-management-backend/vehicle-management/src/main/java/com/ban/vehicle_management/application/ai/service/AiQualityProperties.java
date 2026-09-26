package com.ban.vehicle_management.application.ai.service;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.quality")
public class AiQualityProperties {

    private Duration jobStuckTolerance = Duration.ofMinutes(30);
    private Duration candidateStaleTolerance = Duration.ofMinutes(60);

    @AssertTrue(message = "AI quality tolerances must be greater than zero")
    public boolean areTolerancesValid() {
        return isPositive(jobStuckTolerance) && isPositive(candidateStaleTolerance);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
