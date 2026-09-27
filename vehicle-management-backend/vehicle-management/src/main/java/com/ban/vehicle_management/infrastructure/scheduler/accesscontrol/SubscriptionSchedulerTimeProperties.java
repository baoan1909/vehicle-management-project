package com.ban.vehicle_management.infrastructure.scheduler.accesscontrol;

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
@ConfigurationProperties(prefix = "app.scheduler.subscription-lifecycle")
public class SubscriptionSchedulerTimeProperties {

    @NotNull
    private Duration fixedDelay = Duration.ofMinutes(5);

    @NotNull
    private Duration initialDelay = Duration.ofMinutes(1);

    @AssertTrue(message = "Subscription scheduler delays must be greater than zero")
    public boolean areDelaysValid() {
        return isPositive(fixedDelay) && isPositive(initialDelay);
    }

    private boolean isPositive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
