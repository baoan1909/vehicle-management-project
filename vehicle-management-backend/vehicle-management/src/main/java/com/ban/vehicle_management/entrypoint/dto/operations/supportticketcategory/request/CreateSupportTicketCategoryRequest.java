package com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.request;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType;
import com.ban.vehicle_management.shared.enumeration.operations.PermissionMatchMode;
import java.util.Set;

public record CreateSupportTicketCategoryRequest(
        String code,
        String name,
        String description,
        SupportTicketCategoryPriority priority,
        SupportTicketAudienceType audienceType,
        String featureCode,
        PermissionMatchMode permissionMatchMode,
        Integer displayOrder,
        Set<String> requiredPermissionCodes
) {
    public CreateSupportTicketCategoryRequest(
            String code, String name, String description, SupportTicketCategoryPriority priority
    ) {
        this(code, name, description, priority, null, null, null, null, Set.of());
    }
}
