package com.ban.vehicle_management.application.billing.payment.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.authorization.PaymentAccessGuard;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentUseCaseImplTest {

    @Mock
    private PaymentPortOut paymentPortOut;

    @Mock
    private InvoicePortOut invoicePortOut;

    @Mock
    private PaymentAccessGuard paymentAccessGuard;

    @InjectMocks
    private PaymentUseCaseImpl paymentUseCase;

    @Test
    void shouldRecordPaymentAndMarkInvoicePaid() {
        UUID invoiceId = UUID.randomUUID();
        UUID receivedBy = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId);
        Payment request = validPayment(PaymentMethod.BANK_TRANSFER);

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(receivedBy);
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentPortOut.existsByTransactionRefAndStatus("VCB202606120001", PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentPortOut.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePortOut.save(invoice)).thenReturn(invoice);

        Payment savedPayment = paymentUseCase.recordPayment(invoiceId, request);

        assertNotNull(savedPayment.getPaymentId());
        assertEquals(invoiceId, savedPayment.getInvoiceId());
        assertEquals(receivedBy, savedPayment.getReceivedBy());
        assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());
        assertNotNull(savedPayment.getPaidAt());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(savedPayment.getPaidAt(), invoice.getPaidAt());
        verify(paymentPortOut).save(savedPayment);
        verify(invoicePortOut).save(invoice);
    }

    @Test
    void shouldAllowCashPaymentWithoutTransactionRef() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId);
        Payment request = validPayment(PaymentMethod.CASH);
        request.setTransactionRef(null);

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(UUID.randomUUID());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentPortOut.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePortOut.save(invoice)).thenReturn(invoice);

        Payment savedPayment = paymentUseCase.recordPayment(invoiceId, request);

        assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());
        verify(paymentPortOut, never()).existsByTransactionRefAndStatus(any(), any());
    }

    @Test
    void shouldThrowWhenInvoiceNotFound() {
        UUID invoiceId = UUID.randomUUID();

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(UUID.randomUUID());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> paymentUseCase.recordPayment(invoiceId, validPayment(PaymentMethod.BANK_TRANSFER)));
        verify(paymentPortOut, never()).save(any(Payment.class));
    }

    @Test
    void shouldRejectPaidInvoice() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.parse("2026-06-12T08:00:00Z"));

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(UUID.randomUUID());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThrows(ConflictException.class, () -> paymentUseCase.recordPayment(invoiceId, validPayment(PaymentMethod.BANK_TRANSFER)));
    }

    @Test
    void shouldRejectCancelledInvoice() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId);
        invoice.setStatus(InvoiceStatus.CANCELLED);

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(UUID.randomUUID());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThrows(ConflictException.class, () -> paymentUseCase.recordPayment(invoiceId, validPayment(PaymentMethod.BANK_TRANSFER)));
    }

    @Test
    void shouldRejectZeroAmountInvoice() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId);
        invoice.setFinalAmount(BigDecimal.ZERO);

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(UUID.randomUUID());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThrows(BadRequestException.class, () -> paymentUseCase.recordPayment(invoiceId, validPayment(PaymentMethod.BANK_TRANSFER)));
    }

    @Test
    void shouldRejectWhenPaymentAmountDoesNotEqualInvoiceFinalAmount() {
        UUID invoiceId = UUID.randomUUID();
        Payment request = validPayment(PaymentMethod.BANK_TRANSFER);
        request.setAmount(new BigDecimal("200000"));

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(UUID.randomUUID());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(unpaidInvoice(invoiceId)));

        assertThrows(BadRequestException.class, () -> paymentUseCase.recordPayment(invoiceId, request));
        verify(paymentPortOut, never()).save(any(Payment.class));
    }

    @Test
    void shouldRejectWhenSuccessfulPaymentAlreadyExistsForInvoice() {
        UUID invoiceId = UUID.randomUUID();

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(UUID.randomUUID());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(unpaidInvoice(invoiceId)));
        when(paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)).thenReturn(true);

        assertThrows(ConflictException.class, () -> paymentUseCase.recordPayment(invoiceId, validPayment(PaymentMethod.BANK_TRANSFER)));
        verify(paymentPortOut, never()).save(any(Payment.class));
    }

    @Test
    void shouldRejectWhenTransactionRefAlreadyExists() {
        UUID invoiceId = UUID.randomUUID();

        when(paymentAccessGuard.requireCanCreateAndGetAccountId()).thenReturn(UUID.randomUUID());
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(unpaidInvoice(invoiceId)));
        when(paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentPortOut.existsByTransactionRefAndStatus("VCB202606120001", PaymentStatus.SUCCESS)).thenReturn(true);

        assertThrows(ConflictException.class, () -> paymentUseCase.recordPayment(invoiceId, validPayment(PaymentMethod.BANK_TRANSFER)));
        verify(paymentPortOut, never()).save(any(Payment.class));
    }

    @Test
    void shouldReturnFilteredPaymentsWithTrimmedKeyword() {
        UUID invoiceId = UUID.randomUUID();
        UUID receivedBy = UUID.randomUUID();
        Instant fromDate = Instant.parse("2026-06-01T00:00:00Z");
        Instant toDate = Instant.parse("2026-06-30T23:59:59Z");
        List<Payment> expectedPayments = List.of(validPayment(PaymentMethod.BANK_TRANSFER));

        when(paymentPortOut.findAll(
                invoiceId,
                PaymentMethod.BANK_TRANSFER,
                PaymentStatus.SUCCESS,
                receivedBy,
                fromDate,
                toDate,
                "VCB"
        )).thenReturn(expectedPayments);

        List<Payment> result = paymentUseCase.getPayments(
                invoiceId,
                PaymentMethod.BANK_TRANSFER,
                PaymentStatus.SUCCESS,
                receivedBy,
                fromDate,
                toDate,
                " VCB "
        );

        assertEquals(expectedPayments, result);
        verify(paymentAccessGuard).ensureCanReadAll();
    }

    private Payment validPayment(PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(new BigDecimal("300000"));
        payment.setTransactionRef("VCB202606120001");
        payment.setNote("Da kiem tra sao ke");
        return payment;
    }

    private Invoice unpaidInvoice(UUID invoiceId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setInvoiceNo("INV-001");
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setAmount(new BigDecimal("300000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("300000"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.parse("2026-06-12T07:00:00Z"));
        return invoice;
    }
}
