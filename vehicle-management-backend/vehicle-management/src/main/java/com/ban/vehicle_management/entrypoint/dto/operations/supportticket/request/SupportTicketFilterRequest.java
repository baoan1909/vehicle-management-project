package com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.UUID;

public record SupportTicketFilterRequest(
        UUID customerId,
        UUID categoryId,
        UUID assignedTo,
        SupportTicketStatus status,
        SupportTicketCategoryPriority priority,
        String keyword
) {
}