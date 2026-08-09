package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.PaymentResponse;
import java.util.List;

public record InvoiceManagementDetailResponse(
        InvoiceManagementItemResponse invoice,
        List<InvoiceLineItemResponse> lineItems,
        List<PaymentResponse> payments
) {
}
