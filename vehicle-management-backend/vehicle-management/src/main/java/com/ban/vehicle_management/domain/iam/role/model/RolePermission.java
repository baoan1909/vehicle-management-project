package com.ban.vehicle_management.domain.iam.role.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission extends AuditableDomainModel {

    private UUID id;
    private UUID roleId;
    private UUID permissionId;
    private Boolean isActive;
    private Boolean isSystem;
}

