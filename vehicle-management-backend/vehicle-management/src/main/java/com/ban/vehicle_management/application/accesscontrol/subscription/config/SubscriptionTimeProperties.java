package com.ban.vehicle_management.application.accesscontrol.subscription.config;

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
@ConfigurationProperties(prefix = "app.subscription")
public class SubscriptionTimeProperties {

    @NotNull
    private Duration paymentTimeout = Duration.ofHours(48);

    @AssertTrue(message = "app.subscription.payment-timeout must be greater than zero")
    public boolean isPaymentTimeoutValid() {
        return paymentTimeout != null && !paymentTimeout.isZero() && !paymentTimeout.isNegative();
    }
}
