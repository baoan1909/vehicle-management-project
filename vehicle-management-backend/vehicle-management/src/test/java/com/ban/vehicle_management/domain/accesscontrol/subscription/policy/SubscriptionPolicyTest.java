package com.ban.vehicle_management.domain.accesscontrol.subscription.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionPolicyTest {

    private final SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();

    @Test
    void shouldInitializeNewSubscriptionWithPendingStatus() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = baseSubscription();
        subscription.setStatus(null);
        subscription.setRequestedEffectiveFrom(currentDate.plusDays(4));

        subscriptionPolicy.initializeNewSubscription(subscription, 30, currentDate);

        assertEquals(SubscriptionStatus.PENDING, subscription.getStatus());
        assertEquals(currentDate.plusDays(4), subscription.getEffectiveFrom());
        assertEquals(currentDate.plusDays(33), subscription.getEffectiveTo());
        assertNull(subscription.getCardId());
    }

    @Test
    void shouldApprovePaidSubscriptionWithPendingPaymentStatus() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);
        UUID approvedBy = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2026-05-15T10:00:00Z");
        UUID reservedCardId = UUID.randomUUID();

        subscriptionPolicy.approve(
                subscription,
                approvedBy,
                approvedAt,
                currentDate,
                reservedCardId,
                new BigDecimal("140000")
        );

        assertEquals(SubscriptionStatus.PENDING_PAYMENT, subscription.getStatus());
        assertEquals(approvedBy, subscription.getApprovedBy());
        assertEquals(approvedAt, subscription.getApprovedAt());
        assertEquals(reservedCardId, subscription.getCardId());
        assertNull(subscription.getCardReceiptDate());
    }

    @Test
    void shouldApproveFreeSubscriptionWithPendingCardStatus() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);

        subscriptionPolicy.approve(
                subscription,
                UUID.randomUUID(),
                Instant.parse("2026-05-15T10:00:00Z"),
                currentDate,
                UUID.randomUUID(),
                BigDecimal.ZERO
        );

        assertEquals(SubscriptionStatus.PENDING_CARD, subscription.getStatus());
    }

    @Test
    void shouldRejectApprovalWhenDeadlineExpired() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);

        assertThrows(ConflictException.class, () -> subscriptionPolicy.approve(
                subscription,
                UUID.randomUUID(),
                Instant.parse("2026-05-15T10:00:00Z"),
                subscription.getRequestedEffectiveFrom(),
                UUID.randomUUID(),
                new BigDecimal("140000")
        ));
    }

    @Test
    void shouldMarkPaymentCompletedWithPendingCardStatus() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = approvedSubscription(currentDate, SubscriptionStatus.PENDING_PAYMENT);

        subscriptionPolicy.markPaymentCompleted(subscription);

        assertEquals(SubscriptionStatus.PENDING_CARD, subscription.getStatus());
    }

    @Test
    void shouldAssignReservedCardAndActivateSubscription() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = approvedSubscription(currentDate, SubscriptionStatus.PENDING_CARD);
        LocalDate cardReceiptDate = currentDate.plusDays(5);

        subscriptionPolicy.assignReservedCard(subscription, cardReceiptDate, 30);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(cardReceiptDate, subscription.getCardReceiptDate());
        assertEquals(cardReceiptDate, subscription.getEffectiveFrom());
        assertEquals(cardReceiptDate.plusDays(29), subscription.getEffectiveTo());
    }

    @Test
    void shouldRejectSubscriptionWithReasonAndReviewer() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);
        UUID rejectedBy = UUID.randomUUID();
        Instant rejectedAt = Instant.parse("2026-06-16T10:00:00Z");

        subscriptionPolicy.reject(subscription, "Invalid request", rejectedBy, rejectedAt);

        assertEquals(SubscriptionStatus.REJECTED, subscription.getStatus());
        assertEquals("Invalid request", subscription.getRejectionReason());
        assertEquals(rejectedBy, subscription.getRejectedBy());
        assertEquals(rejectedAt, subscription.getRejectedAt());
        assertNull(subscription.getCardId());
    }

    @Test
    void shouldCancelPendingPaymentSubscription() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = approvedSubscription(currentDate, SubscriptionStatus.PENDING_PAYMENT);

        subscriptionPolicy.cancelBeforeRefundWorkflow(subscription);

        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
    }

    @Test
    void shouldExpireActiveSubscriptionWhenCurrentDateIsAfterEffectiveTo() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = approvedSubscription(currentDate, SubscriptionStatus.PENDING_CARD);
        subscriptionPolicy.assignReservedCard(subscription, currentDate.plusDays(4), 30);

        subscriptionPolicy.expire(subscription, subscription.getEffectiveTo().plusDays(1));

        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }

    @Test
    void shouldRejectRequestedDateOutsideAllowedWindow() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = baseSubscription();
        subscription.setRequestedEffectiveFrom(currentDate.plusDays(8));

        assertThrows(BadRequestException.class, () -> subscriptionPolicy.initializeNewSubscription(
                subscription,
                30,
                currentDate
        ));
    }

    private Subscription baseSubscription() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setCustomerVehicleId(UUID.randomUUID());
        subscription.setTicketTypeId(UUID.randomUUID());
        subscription.setPriceRuleId(UUID.randomUUID());
        subscription.setPrice(new BigDecimal("150000"));
        return subscription;
    }

    private Subscription initializedPendingSubscription(LocalDate currentDate) {
        Subscription subscription = baseSubscription();
        subscription.setRequestedEffectiveFrom(currentDate.plusDays(4));
        subscriptionPolicy.initializeNewSubscription(subscription, 30, currentDate);
        return subscription;
    }

    private Subscription approvedSubscription(LocalDate currentDate, SubscriptionStatus status) {
        Subscription subscription = initializedPendingSubscription(currentDate);
        subscription.setStatus(status);
        subscription.setApprovedBy(UUID.randomUUID());
        subscription.setApprovedAt(Instant.parse("2026-06-16T10:00:00Z"));
        subscription.setCardId(UUID.randomUUID());
        return subscription;
    }
}

