package com.ban.vehicle_management.application.billing.payment.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.authorization.PaymentAccessGuard;
import com.ban.vehicle_management.application.billing.payment.model.command.CreateVnpayPaymentCommand;
import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayCallbackData;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentLink;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayReturnResult;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.VnpayGatewayPortOut;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingCheckoutCompletionPortIn;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.payment.policy.PaymentPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VnpayPaymentUseCaseImplTest {

    @Mock
    private PaymentPortOut paymentPortOut;
    @Mock
    private InvoicePortOut invoicePortOut;
    @Mock
    private VnpayGatewayPortOut vnpayGatewayPortOut;
    @Mock
    private PaymentAccessGuard paymentAccessGuard;
    @Mock
    private SubscriptionPortIn subscriptionPortIn;
    @Mock
    private ParkingCheckoutCompletionPortIn parkingCheckoutCompletionPortIn;
    @Mock
    private NotificationPortIn notificationPortIn;

    private VnpayPaymentUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new VnpayPaymentUseCaseImpl(
                paymentPortOut,
                invoicePortOut,
                vnpayGatewayPortOut,
                paymentAccessGuard,
                subscriptionPortIn,
                parkingCheckoutCompletionPortIn,
                notificationPortIn,
                new BigDecimal("10000")
        );
    }

    @Test
    void shouldCreatePendingVnpayPayment() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId, null);
        Instant expiresAt = Instant.now().plusSeconds(900);

        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentPortOut.findFirstByInvoiceIdAndStatus(invoiceId, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(vnpayGatewayPortOut.createPaymentLink(any()))
                .thenReturn(new VnpayPaymentLink("https://sandbox.vnpayment.vn/payment", expiresAt));
        when(paymentPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VnpayPaymentResult result = useCase.createPayment(
                invoiceId,
                new CreateVnpayPaymentCommand(null, "vn", "127.0.0.1")
        );

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentPortOut).save(paymentCaptor.capture());
        assertEquals(PaymentStatus.PENDING, paymentCaptor.getValue().getStatus());
        assertEquals(invoice.getFinalAmount(), paymentCaptor.getValue().getAmount());
        assertEquals("https://sandbox.vnpayment.vn/payment", result.paymentUrl());
        assertNotNull(result.transactionRef());
    }

    @Test
    void shouldReusePendingVnpayPaymentWhilePaymentLinkIsActive() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId, null);
        Payment pendingPayment = pendingPayment(
                invoiceId,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(840)
        );

        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentPortOut.findFirstByInvoiceIdAndStatus(invoiceId, PaymentStatus.PENDING))
                .thenReturn(Optional.of(pendingPayment));
        when(vnpayGatewayPortOut.createPaymentLink(any()))
                .thenReturn(new VnpayPaymentLink(
                        "https://sandbox.vnpayment.vn/payment?retry=true",
                        pendingPayment.getExpiresAt()
                ));

        VnpayPaymentResult result = useCase.createPayment(
                invoiceId,
                new CreateVnpayPaymentCommand(null, "vn", "127.0.0.1")
        );

        assertEquals(pendingPayment.getPaymentId(), result.paymentId());
        assertEquals(pendingPayment.getTransactionRef(), result.transactionRef());
        assertEquals(pendingPayment.getExpiresAt(), result.expiresAt());
        assertEquals("https://sandbox.vnpayment.vn/payment?retry=true", result.paymentUrl());
        verify(paymentPortOut, never()).save(any());
    }

    @Test
    void shouldFailExpiredPendingPaymentAndCreateNewPaymentAttempt() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId, null);
        Payment expiredPayment = pendingPayment(
                invoiceId,
                Instant.now().minusSeconds(1800),
                Instant.now().minusSeconds(900)
        );
        Instant newExpiresAt = Instant.now().plusSeconds(900);

        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)).thenReturn(false);
        when(paymentPortOut.findFirstByInvoiceIdAndStatus(invoiceId, PaymentStatus.PENDING))
                .thenReturn(Optional.of(expiredPayment));
        when(vnpayGatewayPortOut.createPaymentLink(any()))
                .thenReturn(new VnpayPaymentLink(
                        "https://sandbox.vnpayment.vn/payment?new=true",
                        newExpiresAt
                ));
        when(paymentPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VnpayPaymentResult result = useCase.createPayment(
                invoiceId,
                new CreateVnpayPaymentCommand(null, "vn", "127.0.0.1")
        );

        assertEquals(PaymentStatus.FAILED, expiredPayment.getStatus());
        assertEquals("EXPIRED", expiredPayment.getProviderResponseCode());
        assertNotEquals(expiredPayment.getPaymentId(), result.paymentId());
        assertEquals("https://sandbox.vnpayment.vn/payment?new=true", result.paymentUrl());
        verify(paymentPortOut, times(2)).save(any());
    }

    @Test
    void shouldCompleteInvoiceAndSubscriptionFromSuccessfulIpn() {
        UUID invoiceId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId, subscriptionId);
        Payment payment = pendingPayment(invoiceId);
        Instant paidAt = Instant.parse("2026-07-25T03:00:00Z");

        when(vnpayGatewayPortOut.verifyCallback(any())).thenReturn(new VnpayCallbackData(
                true,
                "TESTCODE",
                payment.getTransactionRef(),
                payment.getAmount(),
                "00",
                "00",
                "123456789",
                "NCB",
                "ATM",
                paidAt
        ));
        when(paymentPortOut.findByTransactionRefForUpdate(payment.getTransactionRef()))
                .thenReturn(Optional.of(payment));
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.processIpn(new VnpayCallbackCommand(Map.of("vnp_TxnRef", payment.getTransactionRef())));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(paidAt, invoice.getPaidAt());
        verify(subscriptionPortIn).markSubscriptionPaymentCompleted(subscriptionId);
    }

    @Test
    void shouldCompleteInvoiceAndSubscriptionFromSuccessfulReturn() {
        UUID invoiceId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Invoice invoice = unpaidInvoice(invoiceId, subscriptionId);
        Payment payment = pendingPayment(invoiceId);
        Instant paidAt = Instant.parse("2026-07-25T03:00:00Z");

        when(vnpayGatewayPortOut.verifyCallback(any())).thenReturn(new VnpayCallbackData(
                true,
                "TESTCODE",
                payment.getTransactionRef(),
                payment.getAmount(),
                "00",
                "00",
                "123456789",
                "NCB",
                "ATM",
                paidAt
        ));
        when(paymentPortOut.findByTransactionRefForUpdate(payment.getTransactionRef()))
                .thenReturn(Optional.of(payment));
        when(invoicePortOut.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VnpayReturnResult result = useCase.verifyReturn(
                new VnpayCallbackCommand(Map.of("vnp_TxnRef", payment.getTransactionRef()))
        );

        assertEquals(PaymentStatus.SUCCESS, result.paymentStatus());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(paidAt, invoice.getPaidAt());
        verify(subscriptionPortIn).markSubscriptionPaymentCompleted(subscriptionId);
    }

    @Test
    void shouldMarkPendingPaymentFailedWhenCustomerCancelsAtVnpay() {
        UUID invoiceId = UUID.randomUUID();
        Payment payment = pendingPayment(
                invoiceId,
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(840)
        );

        when(vnpayGatewayPortOut.verifyCallback(any())).thenReturn(new VnpayCallbackData(
                true,
                "TESTCODE",
                payment.getTransactionRef(),
                payment.getAmount(),
                "24",
                "02",
                null,
                "NCB",
                "ATM",
                null
        ));
        when(paymentPortOut.findByTransactionRefForUpdate(payment.getTransactionRef()))
                .thenReturn(Optional.of(payment));
        when(paymentPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VnpayReturnResult result = useCase.verifyReturn(
                new VnpayCallbackCommand(Map.of("vnp_TxnRef", payment.getTransactionRef()))
        );

        assertEquals(PaymentStatus.FAILED, result.paymentStatus());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("24", payment.getProviderResponseCode());
        verify(paymentPortOut).save(payment);
    }

    private Invoice unpaidInvoice(UUID invoiceId, UUID subscriptionId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setInvoiceNo("INV-2026-001");
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setSubscriptionId(subscriptionId);
        invoice.setAmount(new BigDecimal("50000.00"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("50000.00"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.parse("2026-07-25T02:00:00Z"));
        return invoice;
    }

    private Payment pendingPayment(UUID invoiceId) {
        return pendingPayment(
                invoiceId,
                Instant.parse("2026-07-25T02:00:00Z"),
                Instant.parse("2026-07-25T02:15:00Z")
        );
    }

    private Payment pendingPayment(UUID invoiceId, Instant createdAt, Instant expiresAt) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        new PaymentPolicy().initializePendingVnpayPayment(
                payment,
                invoiceId,
                new BigDecimal("50000.00"),
                "VNP123",
                createdAt,
                expiresAt
        );
        return payment;
    }
}
