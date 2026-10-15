package com.ban.vehicle_management.application.billing.invoice.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.invoice.authorization.InvoiceAccessGuard;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceOwnListLimitTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

    @Mock
    private InvoicePortOut invoicePortOut;

    @Mock
    private InvoiceAccessGuard invoiceAccessGuard;

    @Mock
    private PaymentPortOut paymentPortOut;

    @Mock
    private ParkingSessionPortOut parkingSessionPortOut;

    @InjectMocks
    private InvoiceUseCaseImpl invoiceUseCase;

    @Test
    void shouldQueryOwnInvoicesOnceWithDefaultLimit() {
        UUID subscriptionId = UUID.randomUUID();
        when(invoiceAccessGuard.resolveCustomerIdForList(null)).thenReturn(CUSTOMER_ID);
        when(invoicePortOut.findOwnInvoices(eq(CUSTOMER_ID), eq(List.of(subscriptionId)),
                eq(null), eq(null), eq(null), eq(null), eq(100)))
                .thenReturn(List.of());

        assertEquals(List.of(), invoiceUseCase.getOwnInvoices(List.of(subscriptionId), null, null, null, null, null));
    }

    @Test
    void shouldClampOwnInvoiceLimit() {
        when(invoiceAccessGuard.resolveCustomerIdForList(null)).thenReturn(CUSTOMER_ID);
        when(invoicePortOut.findOwnInvoices(eq(CUSTOMER_ID), eq(List.of()),
                eq(null), eq(null), eq(null), eq(null), eq(200)))
                .thenReturn(List.of());

        invoiceUseCase.getOwnInvoices(List.of(), null, null, null, null, 9999);
        verify(invoicePortOut).findOwnInvoices(eq(CUSTOMER_ID), eq(List.of()),
                eq(null), eq(null), eq(null), eq(null), eq(200));
    }

    @Test
    void shouldDedupeAndCapSubscriptionIds() {
        UUID subscriptionId = UUID.randomUUID();
        when(invoiceAccessGuard.resolveCustomerIdForList(null)).thenReturn(CUSTOMER_ID);
        when(invoicePortOut.findOwnInvoices(eq(CUSTOMER_ID), eq(List.of(subscriptionId)),
                eq(null), eq(null), eq(null), eq(null), eq(100)))
                .thenReturn(List.of(new Invoice()));

        List<Invoice> invoices = invoiceUseCase.getOwnInvoices(
                new java.util.ArrayList<>(java.util.Arrays.asList(subscriptionId, subscriptionId, null)),
                null, null, null, null, null);

        assertEquals(1, invoices.size());
    }
}
