package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionPaymentTimeoutUseCaseImplTest {

    @Mock
    private SubscriptionPortOut subscriptionPortOut;

    @Mock
    private InvoicePortOut invoicePortOut;

    @Mock
    private PaymentPortOut paymentPortOut;

    @Mock
    private CardPortOut cardPortOut;

    private SubscriptionPaymentTimeoutUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubscriptionPaymentTimeoutUseCaseImpl(
                subscriptionPortOut,
                invoicePortOut,
                paymentPortOut,
                cardPortOut,
                48
        );
    }

    @Test
    void shouldCancelExpiredSubscriptionInvoicePaymentAndReleaseCard() {
        Instant now = Instant.parse("2026-07-28T03:00:00Z");
        Subscription subscription = pendingPaymentSubscription();
        Invoice invoice = unpaidInvoice(subscription.getSubscriptionId());
        Payment payment = pendingVnpayPayment(invoice.getInvoiceId());
        Card card = reservedCard(subscription.getCardId());

        when(subscriptionPortOut.findExpiredPendingPaymentsForUpdate(
                Instant.parse("2026-07-26T03:00:00Z"),
                LocalDate.of(2026, 7, 28)
        )).thenReturn(List.of(subscription));
        when(invoicePortOut.findFirstBySubscriptionIdAndStatus(
                subscription.getSubscriptionId(),
                InvoiceStatus.PAID
        )).thenReturn(Optional.empty());
        when(invoicePortOut.findFirstBySubscriptionIdAndStatus(
                subscription.getSubscriptionId(),
                InvoiceStatus.UNPAID
        )).thenReturn(Optional.of(invoice));
        when(paymentPortOut.findByInvoiceId(invoice.getInvoiceId())).thenReturn(List.of(payment));
        when(cardPortOut.findByIdForUpdate(card.getCardId())).thenReturn(Optional.of(card));

        int cancelledCount = useCase.cancelExpiredPendingPayments(now);

        assertEquals(1, cancelledCount);
        assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
        assertNull(subscription.getCardId());
        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("PAYMENT_TIMEOUT", payment.getProviderResponseCode());
        assertEquals("EXPIRED", payment.getProviderTransactionStatus());
        assertEquals(CardStatus.AVAILABLE, card.getStatus());
        verify(paymentPortOut).save(payment);
        verify(invoicePortOut).save(invoice);
        verify(cardPortOut).save(card);
        verify(subscriptionPortOut).save(subscription);
    }

    @Test
    void shouldKeepSubscriptionAndCardWhenInvoiceWasAlreadyPaid() {
        Instant now = Instant.parse("2026-07-28T03:00:00Z");
        Subscription subscription = pendingPaymentSubscription();
        Invoice paidInvoice = unpaidInvoice(subscription.getSubscriptionId());
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setPaidAt(now.minusSeconds(10));

        when(subscriptionPortOut.findExpiredPendingPaymentsForUpdate(any(), any()))
                .thenReturn(List.of(subscription));
        when(invoicePortOut.findFirstBySubscriptionIdAndStatus(
                subscription.getSubscriptionId(),
                InvoiceStatus.PAID
        )).thenReturn(Optional.of(paidInvoice));

        int cancelledCount = useCase.cancelExpiredPendingPayments(now);

        assertEquals(0, cancelledCount);
        assertEquals(SubscriptionStatus.PENDING_CARD, subscription.getStatus());
        verify(subscriptionPortOut).save(subscription);
        verify(invoicePortOut, never()).save(any());
        verify(paymentPortOut, never()).save(any());
        verify(cardPortOut, never()).save(any());
    }

    @Test
    void shouldDoNothingWhenThereAreNoExpiredSubscriptions() {
        when(subscriptionPortOut.findExpiredPendingPaymentsForUpdate(any(), any()))
                .thenReturn(List.of());

        int cancelledCount = useCase.cancelExpiredPendingPayments(Instant.parse("2026-07-28T03:00:00Z"));

        assertEquals(0, cancelledCount);
        verify(subscriptionPortOut, never()).save(any());
        verify(invoicePortOut, never()).save(any());
        verify(paymentPortOut, never()).save(any());
        verify(cardPortOut, never()).save(any());
    }

    private Subscription pendingPaymentSubscription() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setCustomerId(UUID.randomUUID());
        subscription.setCustomerVehicleId(UUID.randomUUID());
        subscription.setCardId(UUID.randomUUID());
        subscription.setTicketTypeId(UUID.randomUUID());
        subscription.setPriceRuleId(UUID.randomUUID());
        subscription.setRequestedEffectiveFrom(LocalDate.of(2026, 7, 30));
        subscription.setEffectiveFrom(LocalDate.of(2026, 7, 30));
        subscription.setEffectiveTo(LocalDate.of(2026, 8, 28));
        subscription.setPrice(new BigDecimal("80000"));
        subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
        subscription.setApprovedBy(UUID.randomUUID());
        subscription.setApprovedAt(Instant.parse("2026-07-25T03:00:00Z"));
        return subscription;
    }

    private Invoice unpaidInvoice(UUID subscriptionId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setInvoiceNo("INV-202607280001");
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setSubscriptionId(subscriptionId);
        invoice.setAmount(new BigDecimal("80000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("80000"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.parse("2026-07-25T03:00:00Z"));
        return invoice;
    }

    private Payment pendingVnpayPayment(UUID invoiceId) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setInvoiceId(invoiceId);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setAmount(new BigDecimal("80000"));
        payment.setTransactionRef("VNP-SUBSCRIPTION-TIMEOUT-TEST");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setNote("VNPAY online payment");
        payment.setCreatedAt(Instant.parse("2026-07-25T03:00:00Z"));
        payment.setExpiresAt(Instant.parse("2026-07-25T03:15:00Z"));
        return payment;
    }

    private Card reservedCard(UUID cardId) {
        Card card = new Card();
        card.setCardId(cardId);
        card.setCardNumber("CARD-001");
        card.setUid("RFID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setStatus(CardStatus.RESERVED);
        return card;
    }
}
