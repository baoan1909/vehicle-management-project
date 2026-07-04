package com.ban.vehicle_management.domain.billing.invoice.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvoicePolicyTest {

    private final InvoicePolicy invoicePolicy = new InvoicePolicy();

    @Test
    void shouldInitializeUnpaidInvoiceWhenFinalAmountIsPositive() {
        Invoice invoice = validInvoice();
        invoice.setDiscountAmount(null);
        Instant issuedAt = Instant.parse("2026-06-11T03:00:00Z");

        invoicePolicy.initializeNewInvoice(invoice, "INV-001", issuedAt);

        assertEquals("INV-001", invoice.getInvoiceNo());
        assertEquals(issuedAt, invoice.getIssuedAt());
        assertEquals(new BigDecimal("0"), invoice.getDiscountAmount());
        assertEquals(new BigDecimal("300000"), invoice.getFinalAmount());
        assertEquals(InvoiceStatus.UNPAID, invoice.getStatus());
        assertNull(invoice.getPaidAt());
    }

    @Test
    void shouldInitializePaidInvoiceWhenFinalAmountIsZero() {
        Invoice invoice = validInvoice();
        invoice.setAmount(new BigDecimal("300000"));
        invoice.setDiscountAmount(new BigDecimal("300000"));
        Instant issuedAt = Instant.parse("2026-06-11T03:00:00Z");

        invoicePolicy.initializeNewInvoice(invoice, "INV-002", issuedAt);

        assertEquals(new BigDecimal("0"), invoice.getFinalAmount());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(issuedAt, invoice.getPaidAt());
    }

    @Test
    void shouldRejectWhenDiscountAmountIsGreaterThanAmount() {
        Invoice invoice = validInvoice();
        invoice.setAmount(new BigDecimal("100000"));
        invoice.setDiscountAmount(new BigDecimal("100001"));

        assertThrows(
                BadRequestException.class,
                () -> invoicePolicy.initializeNewInvoice(invoice, "INV-003", Instant.now())
        );
    }

    @Test
    void shouldRejectWhenInvoiceHasMoreThanOneBusinessSource() {
        Invoice invoice = validInitializedInvoice();
        invoice.setParkingSessionId(UUID.randomUUID());
        invoice.setSubscriptionId(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> invoicePolicy.validateState(invoice));
    }

    @Test
    void shouldCancelUnpaidInvoice() {
        Invoice invoice = validInitializedInvoice();

        invoicePolicy.cancel(invoice);

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
    }

    @Test
    void shouldDoNothingWhenCancellingAlreadyCancelledInvoice() {
        Invoice invoice = validInitializedInvoice();
        invoice.setStatus(InvoiceStatus.CANCELLED);

        invoicePolicy.cancel(invoice);

        assertEquals(InvoiceStatus.CANCELLED, invoice.getStatus());
    }

    @Test
    void shouldRejectCancelWhenInvoiceIsPaid() {
        Invoice invoice = validInitializedInvoice();
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.parse("2026-06-11T04:00:00Z"));

        assertThrows(BadRequestException.class, () -> invoicePolicy.cancel(invoice));
    }

    @Test
    void shouldMarkUnpaidInvoiceAsPaid() {
        Invoice invoice = validInitializedInvoice();
        Instant paidAt = Instant.parse("2026-06-11T04:00:00Z");

        invoicePolicy.markPaid(invoice, paidAt);

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(paidAt, invoice.getPaidAt());
    }

    @Test
    void shouldRejectStateWhenFinalAmountDoesNotMatchFormula() {
        Invoice invoice = validInitializedInvoice();
        invoice.setFinalAmount(new BigDecimal("1"));

        assertThrows(BadRequestException.class, () -> invoicePolicy.validateState(invoice));
    }

    private Invoice validInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setAmount(new BigDecimal("300000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        return invoice;
    }

    private Invoice validInitializedInvoice() {
        Invoice invoice = validInvoice();
        invoice.setInvoiceNo("INV-001");
        invoice.setFinalAmount(new BigDecimal("300000"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.parse("2026-06-11T03:00:00Z"));
        invoice.setPaidAt(null);
        return invoice;
    }
}
