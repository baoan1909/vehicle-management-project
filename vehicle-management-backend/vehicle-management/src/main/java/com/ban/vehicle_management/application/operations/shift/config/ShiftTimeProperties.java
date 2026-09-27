package com.ban.vehicle_management.application.operations.shift.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
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
@ConfigurationProperties(prefix = "app.operations.shift")
public class ShiftTimeProperties {

    @NotNull
    private Duration requiredDuration = Duration.ofHours(8);

    @NotNull
    private Duration minimumRest = Duration.ofHours(8);

    @AssertTrue(message = "Shift durations must be greater than zero")
    public boolean areDurationsValid() {
        return isPositive(requiredDuration) && isPositive(minimumRest);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
