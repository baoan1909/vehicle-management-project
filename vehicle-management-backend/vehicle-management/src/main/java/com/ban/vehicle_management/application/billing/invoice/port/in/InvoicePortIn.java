package com.ban.vehicle_management.application.billing.invoice.port.in;

import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementDetailResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementPageResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementSummaryResult;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InvoicePortIn {

    Invoice createInvoice(Invoice invoice);

    InvoiceDetail getInvoiceById(UUID invoiceId);

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

    InvoiceManagementPageResult getManagementInvoices(
            InvoiceStatus status,
            PaymentMethod paymentMethod,
            Instant fromDate,
            Instant toDate,
            String keyword,
            int page,
            int size
    );

    InvoiceManagementSummaryResult getManagementSummary();

    InvoiceManagementDetailResult getManagementInvoiceDetail(UUID invoiceId);

    Invoice cancelInvoice(UUID invoiceId);
}
