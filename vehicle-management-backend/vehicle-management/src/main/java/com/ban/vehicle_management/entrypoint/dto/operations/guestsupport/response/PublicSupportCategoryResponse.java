package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import java.util.UUID;

public record PublicSupportCategoryResponse(
        UUID categoryId,
        String code,
        String name,
        String description,
        SupportTicketCategoryPriority priority
) {
}
