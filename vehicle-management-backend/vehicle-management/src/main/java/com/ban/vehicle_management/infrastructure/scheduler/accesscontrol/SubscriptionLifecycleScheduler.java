package com.ban.vehicle_management.infrastructure.scheduler.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionExpirationPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPaymentTimeoutPortIn;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionLifecycleScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionLifecycleScheduler.class);

    private final SubscriptionExpirationPortIn subscriptionExpirationPortIn;
    private final SubscriptionPaymentTimeoutPortIn subscriptionPaymentTimeoutPortIn;

    public SubscriptionLifecycleScheduler(
            SubscriptionExpirationPortIn subscriptionExpirationPortIn,
            SubscriptionPaymentTimeoutPortIn subscriptionPaymentTimeoutPortIn
    ) {
        this.subscriptionExpirationPortIn = subscriptionExpirationPortIn;
        this.subscriptionPaymentTimeoutPortIn = subscriptionPaymentTimeoutPortIn;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.subscription-lifecycle.fixed-delay-ms:300000}",
            initialDelayString = "${app.scheduler.subscription-lifecycle.initial-delay-ms:60000}"
    )
    public void processSubscriptionLifecycle() {
        expireActiveSubscriptions();
        cancelExpiredPendingPayments();
    }

    private void expireActiveSubscriptions() {
        try {
            int expiredCount = subscriptionExpirationPortIn.expireExpiredSubscriptions();
            if (expiredCount > 0) {
                LOGGER.info("Subscription lifecycle expired {} active subscription(s)", expiredCount);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Subscription lifecycle failed to expire active subscriptions", exception);
        }
    }

    private void cancelExpiredPendingPayments() {
        try {
            int cancelledCount = subscriptionPaymentTimeoutPortIn.cancelExpiredPendingPayments(Instant.now());
            if (cancelledCount > 0) {
                LOGGER.info("Subscription lifecycle cancelled {} expired pending payment(s)", cancelledCount);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Subscription lifecycle failed to cancel expired pending payments", exception);
        }
    }
}
