package com.ban.vehicle_management.entrypoint.dto.operations.supportticketcategory.response;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupportTicketCategoryUserResponse {
    private UUID categoryId;
    private String code;
    private String name;
    private String description;
    private SupportTicketCategoryPriority priority;
    private String featureCode;
    private Integer displayOrder;
}
