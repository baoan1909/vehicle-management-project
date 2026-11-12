package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.iam.account.model.CustomerDeletionInvoiceContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.PhysicalCardFulfillmentStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionRenewalStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerAccountDeletionPolicyTest {
    private final CustomerAccountDeletionPolicy policy = new CustomerAccountDeletionPolicy();

    @Test
    void activeSubscriptionIsAutomaticActionNotBlocker() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        var action = policy.classifyActiveSubscription(subscription, Instant.parse("2026-11-20T00:00:00Z"));

        assertEquals("TERMINATE_ACTIVE_SUBSCRIPTION", action.code());
        assertNull(action.estimatedRefundAmount());
    }

    @Test
    void unpaidPendingSubscriptionPurchaseIsAutomaticallyCancelled() {
        var decision = policy.classifyInvoice(context(
                InvoicePurpose.SUBSCRIPTION_PURCHASE, false, SubscriptionStatus.PENDING_PAYMENT,
                null, null, null, false, null), null);

        assertTrue(decision.blockers().isEmpty());
        assertEquals("CANCEL_UNPAID_SUBSCRIPTION_PURCHASE", decision.automaticActions().getFirst().code());
    }

    @Test
    void consumedParkingInvoiceRemainsBlocker() {
        var decision = policy.classifyInvoice(context(
                InvoicePurpose.PARKING_SESSION, false, null, null, null, null, false, null), null);

        assertTrue(decision.automaticActions().isEmpty());
        assertEquals("UNPAID_CONSUMED_PARKING_INVOICE", decision.blockers().getFirst().code());
    }

    @Test
    void pendingPaymentAlwaysBlocksAutomaticCancellation() {
        var decision = policy.classifyInvoice(context(
                InvoicePurpose.RESERVATION_PREPAYMENT, true, null, null, null,
                CustomerReservationStatus.PENDING_PAYMENT, false, null), null);

        assertEquals("PENDING_PAYMENT", decision.blockers().getFirst().code());
    }

    @Test
    void unfulfilledLostCardRequestIsAutomaticallyCancelled() {
        var decision = policy.classifyInvoice(context(
                InvoicePurpose.LOST_CARD, false, null, null, null, null,
                false, LostCardReportStatus.OPEN), null);

        assertEquals("CANCEL_UNFULFILLED_LOST_CARD_REQUEST", decision.automaticActions().getFirst().code());
    }

    private CustomerDeletionInvoiceContext context(
            InvoicePurpose purpose,
            boolean pendingPayment,
            SubscriptionStatus subscriptionStatus,
            PhysicalCardFulfillmentStatus cardStatus,
            SubscriptionRenewalStatus renewalStatus,
            CustomerReservationStatus reservationStatus,
            boolean reservationConsumed,
            LostCardReportStatus reportStatus) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setPurpose(purpose);
        return new CustomerDeletionInvoiceContext(invoice, pendingPayment, subscriptionStatus,
                cardStatus, renewalStatus, reservationStatus, reservationConsumed, reportStatus);
    }
}
