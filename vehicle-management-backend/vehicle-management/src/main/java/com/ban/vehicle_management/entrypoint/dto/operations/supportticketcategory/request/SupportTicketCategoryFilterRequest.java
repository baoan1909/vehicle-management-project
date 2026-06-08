package com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;

public record SupportTicketCategoryFilterRequest(
        SupportTicketCategoryStatus status,
        SupportTicketCategoryPriority priority,
        String keyword
) {
}