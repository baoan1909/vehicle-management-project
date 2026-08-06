package com.ban.vehicle_management.application.billing.invoice.model.result;

import java.util.List;

public record InvoiceManagementPageResult(
        List<InvoiceManagementItemResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
