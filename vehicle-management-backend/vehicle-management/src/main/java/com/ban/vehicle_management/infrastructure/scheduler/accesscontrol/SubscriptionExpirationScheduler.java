package com.ban.vehicle_management.infrastructure.scheduler.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionExpirationPortIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionExpirationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionExpirationScheduler.class);

    private final SubscriptionExpirationPortIn subscriptionExpirationPortIn;

    public SubscriptionExpirationScheduler(SubscriptionExpirationPortIn subscriptionExpirationPortIn) {
        this.subscriptionExpirationPortIn = subscriptionExpirationPortIn;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.subscription-expiration.fixed-delay-ms:3600000}",
            initialDelayString = "${app.scheduler.subscription-expiration.initial-delay-ms:60000}"
    )
    public void expireExpiredSubscriptions() {
        try {
            int expiredCount = subscriptionExpirationPortIn.expireExpiredSubscriptions();
            if (expiredCount > 0) {
                LOGGER.info("SubscriptionExpirationScheduler expired {} subscriptions", expiredCount);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("SubscriptionExpirationScheduler failed to expire subscriptions", exception);
        }
    }
}
