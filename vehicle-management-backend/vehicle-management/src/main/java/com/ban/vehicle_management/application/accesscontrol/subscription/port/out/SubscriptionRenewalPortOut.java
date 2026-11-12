package com.ban.vehicle_management.application.accesscontrol.subscription.port.out;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.SubscriptionRenewal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRenewalPortOut {
    SubscriptionRenewal save(SubscriptionRenewal renewal);
    Optional<SubscriptionRenewal> findByInvoiceId(UUID invoiceId);
    Optional<SubscriptionRenewal> findByInvoiceIdForUpdate(UUID invoiceId);
    Optional<SubscriptionRenewal> findBySubscriptionIdAndIdempotencyKey(UUID subscriptionId, String idempotencyKey);
    Optional<SubscriptionRenewal> findLiveBySubscriptionId(UUID subscriptionId);
    List<SubscriptionRenewal> findScheduledStartingOnOrBefore(LocalDate businessDate);
}
