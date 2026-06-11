package com.ban.vehicle_management.application.billing.invoice.port.in;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InvoicePortIn {

    Invoice createInvoice(Invoice invoice);

    Invoice getInvoiceById(UUID invoiceId);

    List<Invoice> getInvoices(
            UUID customerId,
            UUID parkingSessionId,
            UUID subcriptionId,
            UUID lostCardReportId,
            InvoiceStatus status,
            Instant fromDate,
            Instant toDate,
            String keyword
    );

    Invoice cancelInvoice(UUID invoiceId);
}
