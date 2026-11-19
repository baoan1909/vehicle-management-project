package com.ban.vehicle_management.entrypoint.dto.billing.invoice.request;

import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceSource;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import java.time.Instant;
import java.util.UUID;

public record InvoiceManagementFilterRequest(
        InvoiceSource source,
        InvoiceStatus status,
        PaymentMethod paymentMethod,
        Instant fromDate,
        Instant toDate,
        String keyword,
        Integer page,
        Integer size,
        UUID organizationId,
        UUID parkingLotId
) {
}
