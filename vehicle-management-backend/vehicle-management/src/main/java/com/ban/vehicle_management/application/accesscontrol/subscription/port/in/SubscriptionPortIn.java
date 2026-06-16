package com.ban.vehicle_management.application.accesscontrol.subscription.port.in;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SubscriptionPortIn {

    Subscription createOwnSubscription(Subscription subscription);

    Subscription createSubscriptionForCustomer(Subscription subscription);

    Subscription getSubscriptionById(UUID subscriptionId);

    List<Subscription> getSubscriptions(
            UUID customerId,
            UUID customerVehicleId,
            UUID cardId,
            UUID ticketTypeId,
            SubscriptionStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String keyword
    );

    Subscription updatePendingSubscription(UUID subscriptionId, Subscription subscription);

    Subscription approveSubscription(UUID subscriptionId);

    Subscription rejectSubscription(UUID subscriptionId, String reason);

    Subscription assignReservedCard(UUID subscriptionId);

    Subscription cancelSubscription(UUID subscriptionId);

    Subscription expireSubscription(UUID subscriptionId);

    Subscription markSubscriptionPaymentCompleted(UUID subscriptionId);
}