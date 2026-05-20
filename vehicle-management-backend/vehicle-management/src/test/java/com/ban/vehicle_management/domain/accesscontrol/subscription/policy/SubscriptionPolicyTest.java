package com.ban.vehicle_management.domain.accesscontrol.subscription.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionPolicyTest {

    private final SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();

    @Test
    void shouldInitializeNewSubscriptionWithPendingStatus() {
        Subscription subscription = validSubscription(SubscriptionStatus.PENDING);
        subscription.setStatus(null);

        subscriptionPolicy.initializeNewSubscription(subscription);

        assertEquals(SubscriptionStatus.PENDING, subscription.getStatus());
    }

    @Test
    void shouldActivateSubscriptionWithApprovalMetadata() {
        Subscription subscription = validSubscription(SubscriptionStatus.PENDING);
        UUID approvedBy = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2026-05-15T10:00:00Z");

        subscriptionPolicy.activate(subscription, approvedBy, approvedAt);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(approvedBy, subscription.getApprovedBy());
        assertEquals(approvedAt, subscription.getApprovedAt());
    }

    @Test
    void shouldRejectSubscriptionWithInvalidDateRange() {
        Subscription subscription = validSubscription(SubscriptionStatus.PENDING);
        subscription.setEffectiveTo(subscription.getEffectiveFrom().minusDays(1));

        assertThrows(BadRequestException.class, () -> subscriptionPolicy.validateState(subscription));
    }

    @Test
    void shouldRequireCardIdWhenReceiptDateIsProvided() {
        Subscription subscription = validSubscription(SubscriptionStatus.PENDING);
        subscription.setCardReceiptDate(LocalDate.of(2026, 5, 16));

        assertThrows(BadRequestException.class, () -> subscriptionPolicy.validateState(subscription));
    }

    @Test
    void shouldRejectPendingSubscriptionWithApprovalMetadata() {
        Subscription subscription = validSubscription(SubscriptionStatus.PENDING);
        subscription.setApprovedBy(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> subscriptionPolicy.validateState(subscription));
    }

    @Test
    void shouldRejectAndClearCardAssignment() {
        Subscription subscription = validSubscription(SubscriptionStatus.PENDING);
        subscription.setCardId(UUID.randomUUID());
        subscription.setCardReceiptDate(LocalDate.of(2026, 5, 16));

        subscriptionPolicy.reject(subscription);

        assertEquals(SubscriptionStatus.REJECTED, subscription.getStatus());
        assertNull(subscription.getCardId());
        assertNull(subscription.getCardReceiptDate());
    }

    private Subscription validSubscription(SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setCustomerVehicleId(UUID.randomUUID());
        subscription.setTicketTypeId(UUID.randomUUID());
        subscription.setEffectiveFrom(LocalDate.of(2026, 5, 15));
        subscription.setEffectiveTo(LocalDate.of(2026, 6, 14));
        subscription.setPrice(new BigDecimal("150000"));
        subscription.setStatus(status);
        return subscription;
    }
}

