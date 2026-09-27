package com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupportTicketCategoryAdminResponse {
    private UUID categoryId;
    private String code;
    private String name;
    private String description;
    private SupportTicketCategoryPriority priority;
    private SupportTicketCategoryStatus status;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
