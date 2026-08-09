package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

public record InvoiceManagementSummaryResponse(
        long total,
        long unpaid,
        long paid,
        long cancelled,
        long refunded
) {
}
