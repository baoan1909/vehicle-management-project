package com.ban.vehicle_management.application.billing.payment.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.model.command.SepayWebhookCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayPaymentInstruction;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayWebhookData;
import com.ban.vehicle_management.application.billing.payment.port.in.PaymentCompletionPortIn;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.SepayGatewayPortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.SepayQuotaPortOut;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPaymentPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPortIn;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.payment.policy.PaymentPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SepayPaymentUseCaseImplTest {
    @Mock private PaymentPortOut payments;
    @Mock private InvoicePortOut invoices;
    @Mock private SepayGatewayPortOut gateway;
    @Mock private SepayQuotaPortOut quota;
    @Mock private CustomerReservationPortIn reservations;
    @Mock private CustomerReservationPaymentPortIn reservationPayments;
    @Mock private PaymentCompletionPortIn paymentCompletion;

    private SepayPaymentUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SepayPaymentUseCaseImpl(
                payments, invoices, gateway, quota, reservations,
                reservationPayments, paymentCompletion, BigDecimal.valueOf(10_000));
        lenient().when(gateway.transactionPrefix()).thenReturn("CPR");
    }

    @Test
    void createsSepayPaymentForAmountBelowTenThousand() {
        UUID reservationId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = unpaidReservationInvoice(invoiceId, "9999");
        Instant expiresAt = Instant.now().plusSeconds(600);
        when(reservations.authorizeGuestPayment(reservationId, "secret")).thenReturn(invoiceId);
        when(invoices.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(payments.findFirstByInvoiceIdAndStatus(invoiceId, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(gateway.monthlyTransactionLimit()).thenReturn(50);
        when(quota.tryAcquire(any(YearMonth.class), any(Integer.class))).thenReturn(true);
        when(gateway.transactionPrefix()).thenReturn("CPR");
        when(gateway.createPaymentInstruction(any(), any(), any()))
                .thenAnswer(invocation -> new SepayPaymentInstruction(
                        "https://vietqr.app/example", "VCB", "0123", "TEST",
                        invocation.getArgument(0), expiresAt));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.createGuestReservationPayment(reservationId, "secret");

        assertEquals("https://vietqr.app/example", result.qrCodeUrl());
        assertTrue(result.transactionRef().matches("CPR\\d{8}"));
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(captor.capture());
        assertEquals(PaymentMethod.SEPAY, captor.getValue().getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, captor.getValue().getStatus());
        assertEquals(FundCustody.PLATFORM, captor.getValue().getFundCustody());
    }

    @Test
    void blocksPaymentWhenMonthlyQuotaIsExhausted() {
        UUID reservationId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(reservations.authorizeGuestPayment(reservationId, "secret")).thenReturn(invoiceId);
        when(invoices.findByIdForUpdate(invoiceId))
                .thenReturn(Optional.of(unpaidReservationInvoice(invoiceId, "8000")));
        when(payments.findFirstByInvoiceIdAndStatus(invoiceId, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(gateway.monthlyTransactionLimit()).thenReturn(50);
        when(quota.tryAcquire(any(YearMonth.class), any(Integer.class))).thenReturn(false);

        assertThrows(ConflictException.class,
                () -> useCase.createGuestReservationPayment(reservationId, "secret"));
        verify(payments, never()).save(any());
    }

    @Test
    void completesPaymentFromAuthenticatedWebhook() {
        UUID invoiceId = UUID.randomUUID();
        Payment payment = pendingSepayPayment(invoiceId);
        Invoice invoice = unpaidReservationInvoice(invoiceId, "8000");
        Instant paidAt = Instant.parse("2026-11-13T03:00:00Z");
        SepayWebhookCommand command = new SepayWebhookCommand(new byte[] {1}, "signature", "1");
        when(gateway.verifyWebhook(command)).thenReturn(new SepayWebhookData(
                true, payment.getTransactionRef(), payment.getAmount(), "FT123", "MBBank",
                "0123", "in", paidAt));
        when(payments.findByTransactionRefForUpdate(payment.getTransactionRef()))
                .thenReturn(Optional.of(payment));
        when(payments.existsByPaymentMethodAndProviderTransactionNo(PaymentMethod.SEPAY, "FT123"))
                .thenReturn(false);
        when(invoices.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(payments.save(payment)).thenReturn(payment);

        useCase.processWebhook(command);

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        verify(reservationPayments).onPaymentConfirmed(invoiceId, paidAt);
        verify(paymentCompletion).completePaymentTruth(payment.getPaymentId());
    }

    @Test
    void rejectsWebhookWithWrongAmount() {
        Payment payment = pendingSepayPayment(UUID.randomUUID());
        SepayWebhookCommand command = new SepayWebhookCommand(new byte[] {1}, "signature", "1");
        when(gateway.verifyWebhook(command)).thenReturn(new SepayWebhookData(
                true, payment.getTransactionRef(), BigDecimal.ONE, "FT123", "MBBank",
                "0123", "in", Instant.now()));
        when(payments.findByTransactionRefForUpdate(payment.getTransactionRef()))
                .thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class, () -> useCase.processWebhook(command));
        verify(invoices, never()).save(any());
    }

    @Test
    void acknowledgesAuthenticatedTransactionWithoutReservationCode() {
        SepayWebhookCommand command = new SepayWebhookCommand(new byte[] {1}, "signature", "1");
        when(gateway.verifyWebhook(command)).thenReturn(new SepayWebhookData(
                true, null, null, null, "Vietcombank", "0000000001", "in", Instant.now()));

        useCase.processWebhook(command);

        verify(payments, never()).findByTransactionRefForUpdate(any());
    }

    private Invoice unpaidReservationInvoice(UUID invoiceId, String amount) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setInvoiceNo("RES-001");
        invoice.setPurpose(InvoicePurpose.RESERVATION_PREPAYMENT);
        invoice.setAmount(new BigDecimal(amount));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal(amount));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.now());
        return invoice;
    }

    private Payment pendingSepayPayment(UUID invoiceId) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        new PaymentPolicy().initializePendingSepayPayment(
                payment, invoiceId, new BigDecimal("8000"), "CPR123",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(540));
        payment.setFundCustody(FundCustody.PLATFORM);
        return payment;
    }
}
