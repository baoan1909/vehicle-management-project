package com.ban.vehicle_management.application.billing.invoice.port.in;

import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementDetailResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementPageResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementSummaryResult;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import java.time.Instant;
import java.util.UUID;

public interface InvoiceManagementPortIn {

    InvoiceManagementPageResult getInvoices(
            InvoiceStatus status,
            PaymentMethod paymentMethod,
            Instant fromDate,
            Instant toDate,
            String keyword,
            int page,
            int size
    );

    InvoiceManagementSummaryResult getSummary();

    InvoiceManagementDetailResult getInvoiceDetail(UUID invoiceId);
}
