package com.ban.vehicle_management.infrastructure.scheduling;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPaymentTimeoutPortIn;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubscriptionPaymentTimeoutScheduler {

    private final SubscriptionPaymentTimeoutPortIn subscriptionPaymentTimeoutPortIn;

    public SubscriptionPaymentTimeoutScheduler(
            SubscriptionPaymentTimeoutPortIn subscriptionPaymentTimeoutPortIn
    ) {
        this.subscriptionPaymentTimeoutPortIn = subscriptionPaymentTimeoutPortIn;
    }

    @Scheduled(
            initialDelayString = "${app.subscription.payment-timeout-initial-delay-ms:60000}",
            fixedDelayString = "${app.subscription.payment-timeout-scan-ms:300000}"
    )
    public void cancelExpiredPendingPayments() {
        int cancelledCount = subscriptionPaymentTimeoutPortIn.cancelExpiredPendingPayments(Instant.now());
        if (cancelledCount > 0) {
            log.info("Cancelled {} expired subscription payment(s)", cancelledCount);
        }
    }
}
