package com.ban.vehicle_management.application.notification.notification.config;

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
@ConfigurationProperties(prefix = "app.notification")
public class NotificationTimeProperties {

    @NotNull
    private Duration pendingReplayWindow = Duration.ofHours(6);

    @AssertTrue(message = "app.notification.pending-replay-window must be greater than zero")
    public boolean isPendingReplayWindowValid() {
        return pendingReplayWindow != null
                && !pendingReplayWindow.isZero()
                && !pendingReplayWindow.isNegative();
    }
}
