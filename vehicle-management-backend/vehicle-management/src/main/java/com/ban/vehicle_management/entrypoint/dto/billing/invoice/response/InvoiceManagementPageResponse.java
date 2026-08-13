package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

import java.util.List;

public record InvoiceManagementPageResponse(
        List<InvoiceManagementItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
