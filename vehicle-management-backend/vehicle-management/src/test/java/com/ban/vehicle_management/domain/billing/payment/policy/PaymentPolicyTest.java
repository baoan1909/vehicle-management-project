package com.ban.vehicle_management.domain.billing.payment.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentPolicyTest {

    private final PaymentPolicy paymentPolicy = new PaymentPolicy();

    @Test
    void shouldInitializeSuccessfulNonCashPayment() {
        Payment payment = validPayment(PaymentMethod.BANK_TRANSFER);
        UUID invoiceId = UUID.randomUUID();
        UUID receivedBy = UUID.randomUUID();
        Instant paidAt = Instant.parse("2026-06-12T08:00:00Z");

        paymentPolicy.initializeSuccessfulPayment(payment, invoiceId, receivedBy, paidAt);

        assertEquals(invoiceId, payment.getInvoiceId());
        assertEquals(receivedBy, payment.getReceivedBy());
        assertEquals(paidAt, payment.getPaidAt());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("VCB202606120001", payment.getTransactionRef());
        assertEquals("Da kiem tra sao ke", payment.getNote());
    }

    @Test
    void shouldAllowCashPaymentWithoutTransactionRef() {
        Payment payment = validPayment(PaymentMethod.CASH);
        payment.setTransactionRef(null);

        paymentPolicy.initializeSuccessfulPayment(
                payment,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-06-12T08:00:00Z")
        );

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNull(payment.getTransactionRef());
    }

    @Test
    void shouldRejectNonCashPaymentWithoutTransactionRef() {
        Payment payment = validPayment(PaymentMethod.MOMO);
        payment.setTransactionRef(null);

        assertThrows(
                BadRequestException.class,
                () -> paymentPolicy.initializeSuccessfulPayment(
                        payment,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-06-12T08:00:00Z")
                )
        );
    }

    @Test
    void shouldRejectWhenAmountIsZero() {
        Payment payment = validPayment(PaymentMethod.BANK_TRANSFER);
        payment.setAmount(BigDecimal.ZERO);

        assertThrows(
                BadRequestException.class,
                () -> paymentPolicy.initializeSuccessfulPayment(
                        payment,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-06-12T08:00:00Z")
                )
        );
    }

    @Test
    void shouldRejectWhenAmountIsNegative() {
        Payment payment = validPayment(PaymentMethod.BANK_TRANSFER);
        payment.setAmount(new BigDecimal("-1"));

        assertThrows(
                BadRequestException.class,
                () -> paymentPolicy.initializeSuccessfulPayment(
                        payment,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-06-12T08:00:00Z")
                )
        );
    }

    @Test
    void shouldRejectStateWhenStatusIsNotSuccess() {
        Payment payment = validInitializedPayment();
        payment.setStatus(PaymentStatus.PENDING);

        assertThrows(BadRequestException.class, () -> paymentPolicy.validateState(payment));
    }

    @Test
    void shouldRejectUnsupportedCharactersInNote() {
        Payment payment = validPayment(PaymentMethod.BANK_TRANSFER);
        payment.setNote("<script>");

        assertThrows(
                BadRequestException.class,
                () -> paymentPolicy.initializeSuccessfulPayment(
                        payment,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.parse("2026-06-12T08:00:00Z")
                )
        );
    }

    private Payment validPayment(PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(new BigDecimal("300000"));
        payment.setTransactionRef(" VCB202606120001 ");
        payment.setNote(" Da kiem tra sao ke ");
        return payment;
    }

    private Payment validInitializedPayment() {
        Payment payment = validPayment(PaymentMethod.BANK_TRANSFER);
        payment.setInvoiceId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.parse("2026-06-12T08:00:00Z"));
        payment.setReceivedBy(UUID.randomUUID());
        payment.setTransactionRef("VCB202606120001");
        payment.setNote("Da kiem tra sao ke");
        return payment;
    }
}
