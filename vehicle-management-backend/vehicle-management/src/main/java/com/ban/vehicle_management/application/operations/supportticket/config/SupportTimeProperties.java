package com.ban.vehicle_management.application.operations.supportticket.config;

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
@ConfigurationProperties(prefix = "app.operations.support-ticket")
public class SupportTimeProperties {

    @NotNull
    private Duration escalationRateWindow = Duration.ofHours(24);

    @AssertTrue(message = "app.operations.support-ticket.escalation-rate-window must be greater than zero")
    public boolean isEscalationRateWindowValid() {
        return escalationRateWindow != null
                && !escalationRateWindow.isZero()
                && !escalationRateWindow.isNegative();
    }
}
