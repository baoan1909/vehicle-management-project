package com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;

public record CreateSupportTicketCategoryRequest(
        String code,
        String name,
        String description,
        SupportTicketCategoryPriority priority
) {
}