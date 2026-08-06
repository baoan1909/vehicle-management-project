package com.ban.vehicle_management.application.billing.invoice.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out.LostCardReportPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.authorization.InvoiceAccessGuard;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementPageResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementSummaryResult;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceManagementUseCaseImplTest {

    @Mock private InvoicePortOut invoicePortOut;
    @Mock private PaymentPortOut paymentPortOut;
    @Mock private InvoiceAccessGuard invoiceAccessGuard;
    @Mock private CustomerPortOut customerPortOut;
    @Mock private UserProfilePortOut userProfilePortOut;
    @Mock private CustomerVehiclePortOut customerVehiclePortOut;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;
    @Mock private SubscriptionPortOut subscriptionPortOut;
    @Mock private LostCardReportPortOut lostCardReportPortOut;

    @InjectMocks
    private InvoiceManagementUseCaseImpl useCase;

    @Test
    void shouldFilterInvoicesByDisplayPaymentMethod() {
        Invoice vnpayInvoice = invoice("INV-VNPAY", InvoiceStatus.PAID, "2026-08-06T02:00:00Z");
        Invoice cashInvoice = invoice("INV-CASH", InvoiceStatus.PAID, "2026-08-06T01:00:00Z");

        when(invoicePortOut.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(cashInvoice, vnpayInvoice));
        when(paymentPortOut.findByInvoiceId(vnpayInvoice.getInvoiceId()))
                .thenReturn(List.of(payment(vnpayInvoice.getInvoiceId(), PaymentMethod.VNPAY)));
        when(paymentPortOut.findByInvoiceId(cashInvoice.getInvoiceId()))
                .thenReturn(List.of(payment(cashInvoice.getInvoiceId(), PaymentMethod.CASH)));

        InvoiceManagementPageResult result = useCase.getInvoices(
                null, PaymentMethod.VNPAY, null, null, null, 0, 10
        );

        assertEquals(1, result.totalElements());
        assertEquals("INV-VNPAY", result.items().get(0).invoiceNo());
        assertEquals(PaymentMethod.VNPAY, result.items().get(0).paymentMethod());
        verify(invoiceAccessGuard).ensureCanReadAll();
    }

    @Test
    void shouldBuildInvoiceSummaryFromAllStatuses() {
        when(invoicePortOut.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(
                        invoice("INV-1", InvoiceStatus.UNPAID, "2026-08-06T01:00:00Z"),
                        invoice("INV-2", InvoiceStatus.PAID, "2026-08-06T02:00:00Z"),
                        invoice("INV-3", InvoiceStatus.PAID, "2026-08-06T03:00:00Z"),
                        invoice("INV-4", InvoiceStatus.CANCELLED, "2026-08-06T04:00:00Z"),
                        invoice("INV-5", InvoiceStatus.REFUNDED, "2026-08-06T05:00:00Z")
                ));

        InvoiceManagementSummaryResult result = useCase.getSummary();

        assertEquals(5, result.total());
        assertEquals(1, result.unpaid());
        assertEquals(2, result.paid());
        assertEquals(1, result.cancelled());
        assertEquals(1, result.refunded());
        verify(invoiceAccessGuard).ensureCanReadAll();
    }

    private Invoice invoice(String invoiceNo, InvoiceStatus status, String createdAt) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setInvoiceNo(invoiceNo);
        invoice.setAmount(new BigDecimal("18000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("18000"));
        invoice.setStatus(status);
        invoice.setCreatedAt(Instant.parse(createdAt));
        return invoice;
    }

    private Payment payment(UUID invoiceId, PaymentMethod method) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setInvoiceId(invoiceId);
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(new BigDecimal("18000"));
        payment.setCreatedAt(Instant.parse("2026-08-06T03:00:00Z"));
        return payment;
    }
}
