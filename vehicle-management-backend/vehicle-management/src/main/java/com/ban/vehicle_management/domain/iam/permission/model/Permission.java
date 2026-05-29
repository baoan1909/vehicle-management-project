package com.ban.vehicle_management.domain.iam.permission.model;

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
public class Permission extends AuditableDomainModel {

    private UUID permissionId;
    private String permissionCode;
    private UUID moduleId;
    private UUID actionId;
    private UUID scopeId;
    private String name;
    private String description;
}

