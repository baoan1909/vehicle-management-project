package com.ban.vehicle_management.application.billing.invoice.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.invoice.authorization.InvoiceAccessGuard;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceUseCaseImplTest {

    @Mock
    private InvoicePortOut invoicePortOut;

    @Mock
    private InvoiceAccessGuard invoiceAccessGuard;

    @Mock
    private PaymentPortOut paymentPortOut;

    @InjectMocks
    private InvoiceUseCaseImpl invoiceUseCase;

    @Test
    void shouldCreateManualInvoiceWhenValid() {
        UUID customerId = UUID.randomUUID();
        Invoice request = validCreateRequest();
        request.setCustomerId(customerId);

        when(invoicePortOut.existsCustomerById(customerId)).thenReturn(true);
        when(invoicePortOut.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice createdInvoice = invoiceUseCase.createInvoice(request);

        assertNotNull(createdInvoice.getInvoiceId());
        assertNotNull(createdInvoice.getInvoiceNo());
        assertEquals(InvoiceStatus.UNPAID, createdInvoice.getStatus());
        assertEquals(new BigDecimal("300000"), createdInvoice.getFinalAmount());
        verify(invoiceAccessGuard).ensureCanCreate();
        verify(invoicePortOut).save(createdInvoice);
    }

    @Test
    void shouldCreatePaidInvoiceWhenFinalAmountIsZero() {
        Invoice request = validCreateRequest();
        request.setAmount(new BigDecimal("100000"));
        request.setDiscountAmount(new BigDecimal("100000"));

        when(invoicePortOut.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice createdInvoice = invoiceUseCase.createInvoice(request);

        assertEquals(InvoiceStatus.PAID, createdInvoice.getStatus());
        assertEquals(new BigDecimal("0"), createdInvoice.getFinalAmount());
        assertNotNull(createdInvoice.getPaidAt());
    }

    @Test
    void shouldRejectCreateWhenCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        Invoice request = validCreateRequest();
        request.setCustomerId(customerId);

        when(invoicePortOut.existsCustomerById(customerId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> invoiceUseCase.createInvoice(request));
        verify(invoicePortOut, never()).save(any(Invoice.class));
    }

    @Test
    void shouldRejectCreateWhenParkingSessionDoesNotExist() {
        UUID parkingSessionId = UUID.randomUUID();
        Invoice request = validCreateRequest();
        request.setParkingSessionId(parkingSessionId);

        when(invoicePortOut.existsParkingSessionById(parkingSessionId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> invoiceUseCase.createInvoice(request));
        verify(invoicePortOut, never()).save(any(Invoice.class));
    }

    @Test
    void shouldRejectCreateWhenActiveInvoiceAlreadyExistsForParkingSession() {
        UUID parkingSessionId = UUID.randomUUID();
        Invoice request = validCreateRequest();
        request.setParkingSessionId(parkingSessionId);

        when(invoicePortOut.existsParkingSessionById(parkingSessionId)).thenReturn(true);
        when(invoicePortOut.existsByParkingSessionIdAndStatusIn(eq(parkingSessionId), any(Collection.class)))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> invoiceUseCase.createInvoice(request));
        verify(invoicePortOut, never()).save(any(Invoice.class));
    }

    @Test
    void shouldReturnInvoiceByIdWhenReadable() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = validInitializedInvoice(invoiceId);
        Payment payment = validPayment(invoiceId);

        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.findByInvoiceId(invoiceId)).thenReturn(List.of(payment));

        InvoiceDetail result = invoiceUseCase.getInvoiceById(invoiceId);

        assertEquals(invoiceId, result.getInvoice().getInvoiceId());
        assertEquals(1, result.getPayments().size());
        assertEquals(payment, result.getPayments().get(0));
        verify(invoiceAccessGuard).ensureCanRead(invoice);
    }

    @Test
    void shouldThrowWhenInvoiceNotFound() {
        UUID invoiceId = UUID.randomUUID();

        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> invoiceUseCase.getInvoiceById(invoiceId));
    }

    @Test
    void shouldReturnFilteredInvoicesWithReadableCustomerIdAndTrimmedKeyword() {
        UUID requestedCustomerId = UUID.randomUUID();
        UUID readableCustomerId = UUID.randomUUID();
        List<Invoice> expectedInvoices = List.of(validInitializedInvoice(UUID.randomUUID()));
        Instant fromDate = Instant.parse("2026-06-01T00:00:00Z");
        Instant toDate = Instant.parse("2026-06-30T23:59:59Z");

        when(invoiceAccessGuard.resolveCustomerIdForList(requestedCustomerId)).thenReturn(readableCustomerId);
        when(invoicePortOut.findAll(
                readableCustomerId,
                null,
                null,
                null,
                InvoiceStatus.UNPAID,
                fromDate,
                toDate,
                "INV"
        )).thenReturn(expectedInvoices);

        List<Invoice> result = invoiceUseCase.getInvoices(
                requestedCustomerId,
                null,
                null,
                null,
                InvoiceStatus.UNPAID,
                fromDate,
                toDate,
                " INV "
        );

        assertEquals(expectedInvoices, result);
    }

    @Test
    void shouldCancelInvoice() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = validInitializedInvoice(invoiceId);

        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoicePortOut.save(invoice)).thenReturn(invoice);

        Invoice cancelledInvoice = invoiceUseCase.cancelInvoice(invoiceId);

        assertEquals(InvoiceStatus.CANCELLED, cancelledInvoice.getStatus());
        verify(invoiceAccessGuard).ensureCanCancel();
        verify(invoicePortOut).save(invoice);
    }

    private Invoice validCreateRequest() {
        Invoice invoice = new Invoice();
        invoice.setAmount(new BigDecimal("300000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        return invoice;
    }

    private Invoice validInitializedInvoice(UUID invoiceId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setInvoiceNo("INV-001");
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setAmount(new BigDecimal("300000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("300000"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.parse("2026-06-11T03:00:00Z"));
        return invoice;
    }

    private Payment validPayment(UUID invoiceId) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setInvoiceId(invoiceId);
        payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        payment.setAmount(new BigDecimal("300000"));
        payment.setTransactionRef("VCB202606120001");
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.parse("2026-06-11T04:00:00Z"));
        payment.setReceivedBy(UUID.randomUUID());
        payment.setNote("Da kiem tra sao ke");
        return payment;
    }
}
