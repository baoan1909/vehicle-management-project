package com.ban.vehicle_management.application.billing.invoice.model.result;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import java.util.List;

public record InvoiceManagementDetailResult(
        InvoiceManagementItemResult invoice,
        List<InvoiceLineItemResult> lineItems,
        List<Payment> payments
) {
}
