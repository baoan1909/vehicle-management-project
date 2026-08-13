package com.ban.vehicle_management.entrypoint.dto.billing.invoice.request;

import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import java.time.Instant;

public record InvoiceManagementFilterRequest(
        InvoiceStatus status,
        PaymentMethod paymentMethod,
        Instant fromDate,
        Instant toDate,
        String keyword,
        Integer page,
        Integer size
) {
}
