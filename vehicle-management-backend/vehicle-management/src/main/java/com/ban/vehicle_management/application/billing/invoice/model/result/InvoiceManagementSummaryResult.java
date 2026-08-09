package com.ban.vehicle_management.application.billing.invoice.model.result;

public record InvoiceManagementSummaryResult(
        long total,
        long unpaid,
        long paid,
        long cancelled,
        long refunded
) {
}
