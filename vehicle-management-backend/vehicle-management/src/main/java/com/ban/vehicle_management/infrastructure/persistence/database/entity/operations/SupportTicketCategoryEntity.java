package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType;
import com.ban.vehicle_management.shared.enumeration.operations.PermissionMatchMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "Support_ticket_categories", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketCategoryEntity extends AuditableEntity {

    @Id
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private SupportTicketCategoryPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupportTicketCategoryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 20)
    private SupportTicketAudienceType audienceType;

    @Column(name = "feature_code", length = 100)
    private String featureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_match_mode", nullable = false, length = 10)
    private PermissionMatchMode permissionMatchMode;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;


}
