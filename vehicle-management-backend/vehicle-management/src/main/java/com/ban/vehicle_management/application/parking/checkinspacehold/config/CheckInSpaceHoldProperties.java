package com.ban.vehicle_management.application.parking.checkinspacehold.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.parking.check-in-space-hold")
public class CheckInSpaceHoldProperties {
    private Duration duration = Duration.ofMinutes(2);

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Check-in space hold duration must be positive");
        }
        this.duration = duration;
    }
}
