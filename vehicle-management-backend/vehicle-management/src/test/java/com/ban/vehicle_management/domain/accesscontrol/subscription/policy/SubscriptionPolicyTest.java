package com.ban.vehicle_management.domain.accesscontrol.subscription.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.PhysicalCardFulfillmentStatus;
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
    void shouldAllowSubscriptionToStartToday() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = baseSubscription();
        subscription.setRequestedEffectiveFrom(currentDate);

        subscriptionPolicy.initializeNewSubscription(subscription, 30, currentDate);

        assertEquals(currentDate, subscription.getEffectiveFrom());
        assertEquals(SubscriptionStatus.PENDING, subscription.getStatus());
    }

    @Test
    void shouldSynchronizePhysicalCardStatusWhenPendingPreferenceChanges() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);
        subscription.setPhysicalCardRequested(false);
        subscription.setPhysicalCardDeposit(BigDecimal.ZERO);

        subscriptionPolicy.preparePendingUpdate(subscription, 30, currentDate);

        assertEquals(PhysicalCardFulfillmentStatus.NOT_REQUESTED, subscription.getPhysicalCardStatus());
        assertNull(subscription.getCardId());
        assertNull(subscription.getCardReceiptDate());
    }

    @Test
    void shouldApprovePaidSubscriptionWithPendingPaymentStatus() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);
        UUID approvedBy = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2026-05-15T10:00:00Z");
        subscriptionPolicy.approve(
                subscription,
                approvedBy,
                approvedAt,
                currentDate,
                null,
                new BigDecimal("140000")
        );

        assertEquals(SubscriptionStatus.PENDING_PAYMENT, subscription.getStatus());
        assertEquals(approvedBy, subscription.getApprovedBy());
        assertEquals(approvedAt, subscription.getApprovedAt());
        assertNull(subscription.getCardId());
        assertNull(subscription.getCardReceiptDate());
    }

    @Test
    void shouldActivateFreeSubscriptionWithoutWaitingForPhysicalCard() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);

        subscriptionPolicy.approve(
                subscription,
                UUID.randomUUID(),
                Instant.parse("2026-05-15T10:00:00Z"),
                currentDate,
                null,
                BigDecimal.ZERO
        );

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertNull(subscription.getCardReceiptDate());
    }

    @Test
    void shouldAllowApprovalOnRequestedEffectiveDate() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);

        subscriptionPolicy.approve(
                subscription,
                UUID.randomUUID(),
                Instant.parse("2026-05-15T10:00:00Z"),
                subscription.getRequestedEffectiveFrom(),
                null,
                new BigDecimal("140000")
        );

        assertEquals(SubscriptionStatus.PENDING_PAYMENT, subscription.getStatus());
    }

    @Test
    void shouldRejectApprovalAfterRequestedEffectiveDate() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);

        assertThrows(ConflictException.class, () -> subscriptionPolicy.approve(
                subscription,
                UUID.randomUUID(),
                Instant.parse("2026-05-15T10:00:00Z"),
                subscription.getRequestedEffectiveFrom().plusDays(1),
                UUID.randomUUID(),
                new BigDecimal("140000")
        ));
    }

    @Test
    void shouldActivatePhysicalCardSubscriptionAfterPayment() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = approvedSubscription(currentDate, SubscriptionStatus.PENDING_PAYMENT);

        subscriptionPolicy.markPaymentCompleted(subscription);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertNull(subscription.getCardReceiptDate());
    }

    @Test
    void shouldActivateQrSubscriptionAfterPaymentWithoutWaitingForCard() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);
        subscription.setPhysicalCardRequested(false);
        subscription.setPhysicalCardDeposit(BigDecimal.ZERO);
        subscription.setPhysicalCardStatus(PhysicalCardFulfillmentStatus.NOT_REQUESTED);

        subscriptionPolicy.approve(
                subscription,
                UUID.randomUUID(),
                Instant.parse("2026-05-15T10:00:00Z"),
                currentDate,
                null,
                new BigDecimal("140000")
        );
        subscriptionPolicy.markPaymentCompleted(subscription);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertNull(subscription.getCardId());
        assertNull(subscription.getCardReceiptDate());
    }

    @Test
    void shouldWaitForSpaceAfterQrSubscriptionPaymentAndActivateAfterConfirmation() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = initializedPendingSubscription(currentDate);
        subscription.setPhysicalCardRequested(false);
        subscription.setPhysicalCardDeposit(BigDecimal.ZERO);
        subscription.setPhysicalCardStatus(PhysicalCardFulfillmentStatus.NOT_REQUESTED);

        subscriptionPolicy.approve(subscription, UUID.randomUUID(),
                Instant.parse("2026-06-16T10:00:00Z"), currentDate, null,
                new BigDecimal("140000"));
        subscriptionPolicy.requireSpaceSelection(subscription,
                Instant.parse("2026-06-17T10:00:00Z"));
        subscriptionPolicy.markPaymentCompleted(subscription);

        assertEquals(SubscriptionStatus.PENDING_SPACE, subscription.getStatus());

        Instant confirmedAt = Instant.parse("2026-06-16T11:00:00Z");
        subscriptionPolicy.confirmSpaceSelection(subscription, confirmedAt);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(confirmedAt, subscription.getSpaceSelectionConfirmedAt());
        assertNull(subscription.getSpaceSelectionExpiresAt());
    }

    @Test
    void shouldActivatePhysicalCardSubscriptionAfterSpaceConfirmation() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = approvedSubscription(currentDate, SubscriptionStatus.PENDING_PAYMENT);
        subscriptionPolicy.requireSpaceSelection(subscription,
                Instant.parse("2026-06-17T10:00:00Z"));

        subscriptionPolicy.markPaymentCompleted(subscription);
        assertEquals(SubscriptionStatus.PENDING_SPACE, subscription.getStatus());

        subscriptionPolicy.confirmSpaceSelection(subscription,
                Instant.parse("2026-06-16T12:00:00Z"));
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertNull(subscription.getCardReceiptDate());
    }

    @Test
    void shouldAssignReservedCardWithoutChangingActiveSubscriptionPeriod() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = approvedSubscription(currentDate, SubscriptionStatus.ACTIVE);
        subscription.setCardId(UUID.randomUUID());
        subscription.setPhysicalCardStatus(PhysicalCardFulfillmentStatus.RESERVED);
        LocalDate cardReceiptDate = currentDate.plusDays(5);
        LocalDate effectiveFrom = subscription.getEffectiveFrom();
        LocalDate effectiveTo = subscription.getEffectiveTo();

        subscriptionPolicy.assignReservedCard(subscription, cardReceiptDate);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(cardReceiptDate, subscription.getCardReceiptDate());
        assertEquals(effectiveFrom, subscription.getEffectiveFrom());
        assertEquals(effectiveTo, subscription.getEffectiveTo());
    }

    @Test
    void shouldRequestPhysicalCardForActiveQrSubscription() {
        Subscription subscription = activeQrSubscription(LocalDate.of(2026, 6, 16));

        subscriptionPolicy.requestPhysicalCard(subscription, new BigDecimal("50000"));

        assertEquals(true, subscription.getPhysicalCardRequested());
        assertEquals(new BigDecimal("50000"), subscription.getPhysicalCardDeposit());
        assertEquals(PhysicalCardFulfillmentStatus.PAYMENT_PENDING, subscription.getPhysicalCardStatus());
        assertNull(subscription.getCardId());
    }

    @Test
    void shouldReservePhysicalCardAfterDepositPayment() {
        Subscription subscription = activeQrSubscription(LocalDate.of(2026, 6, 16));
        subscriptionPolicy.requestPhysicalCard(subscription, new BigDecimal("50000"));
        UUID cardId = UUID.randomUUID();

        subscriptionPolicy.reservePhysicalCard(subscription, cardId);

        assertEquals(cardId, subscription.getCardId());
        assertEquals(PhysicalCardFulfillmentStatus.RESERVED, subscription.getPhysicalCardStatus());
    }

    @Test
    void shouldWaitForInventoryWithoutBlockingQrSubscription() {
        Subscription subscription = activeQrSubscription(LocalDate.of(2026, 6, 16));
        subscriptionPolicy.requestPhysicalCard(subscription, new BigDecimal("50000"));

        subscriptionPolicy.waitForPhysicalCardInventory(subscription);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(PhysicalCardFulfillmentStatus.WAITING_INVENTORY, subscription.getPhysicalCardStatus());
        assertNull(subscription.getCardId());
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
        Subscription subscription = approvedSubscription(currentDate, SubscriptionStatus.ACTIVE);

        subscriptionPolicy.expire(subscription, subscription.getEffectiveTo().plusDays(1));

        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }

    @Test
    void shouldTerminateActiveSubscriptionWithoutRefundForAccountDeletion() {
        Subscription subscription = activeQrSubscription(LocalDate.of(2026, 6, 16));
        UUID deletionRequestId = UUID.randomUUID();
        Instant terminatedAt = Instant.parse("2026-07-01T10:00:00Z");

        subscriptionPolicy.terminateForAccountDeletion(subscription, deletionRequestId, terminatedAt);

        assertEquals(SubscriptionStatus.TERMINATED, subscription.getStatus());
        assertEquals(terminatedAt, subscription.getTerminatedAt());
        assertEquals("ACCOUNT_DELETION", subscription.getTerminationReason());
        assertEquals(deletionRequestId, subscription.getAccountDeletionRequestId());
        assertEquals(terminatedAt, subscription.getQrRevokedAt());
        assertEquals(true, subscription.getAccessBlocked());
        assertEquals(terminatedAt, subscription.getAccessBlockedAt());
    }

    @Test
    void shouldNotTerminateSubscriptionThatIsNotActive() {
        Subscription subscription = approvedSubscription(
                LocalDate.of(2026, 6, 16), SubscriptionStatus.EXPIRED);

        assertThrows(ConflictException.class, () -> subscriptionPolicy.terminateForAccountDeletion(
                subscription, UUID.randomUUID(), Instant.parse("2026-07-01T10:00:00Z")));
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

    @Test
    void shouldRejectPeriodQuantityOutsideSupportedRange() {
        LocalDate currentDate = LocalDate.of(2026, 6, 16);
        Subscription subscription = baseSubscription();
        subscription.setPeriodQuantity(13);
        subscription.setRequestedEffectiveFrom(currentDate);

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
        subscription.setCardId(null);
        return subscription;
    }

    private Subscription activeQrSubscription(LocalDate currentDate) {
        Subscription subscription = initializedPendingSubscription(currentDate);
        subscription.setPhysicalCardRequested(false);
        subscription.setPhysicalCardDeposit(BigDecimal.ZERO);
        subscription.setPhysicalCardStatus(PhysicalCardFulfillmentStatus.NOT_REQUESTED);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setApprovedBy(UUID.randomUUID());
        subscription.setApprovedAt(Instant.parse("2026-06-16T10:00:00Z"));
        subscription.setCardId(null);
        subscription.setCardReceiptDate(null);
        return subscription;
    }
}

