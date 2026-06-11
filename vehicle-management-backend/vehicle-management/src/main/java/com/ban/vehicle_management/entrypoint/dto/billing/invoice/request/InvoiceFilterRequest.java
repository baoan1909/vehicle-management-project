package com.ban.vehicle_management.entrypoint.dto.billing.invoice.request;

import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;

import java.time.Instant;
import java.util.UUID;

public record InvoiceFilterRequest (
        UUID customerId,
        UUID parkingSessionId,
        UUID subscriptionId,
        UUID lostCardReportId,
        InvoiceStatus status,
        Instant fromDate,
        Instant toDate,
        String keyword
){
}
