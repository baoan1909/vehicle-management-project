package com.ban.vehicle_management.domain.operations.supportticketcategory.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType;
import com.ban.vehicle_management.shared.enumeration.operations.PermissionMatchMode;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketCategory extends AuditableDomainModel {
    private UUID categoryId;
    private String code;
    private String name;
    private String description;
    private SupportTicketCategoryPriority priority;
    private SupportTicketCategoryStatus status;
    private SupportTicketAudienceType audienceType;
    private String featureCode;
    private PermissionMatchMode permissionMatchMode;
    private Integer displayOrder;
    private Set<String> requiredPermissionCodes;
}
