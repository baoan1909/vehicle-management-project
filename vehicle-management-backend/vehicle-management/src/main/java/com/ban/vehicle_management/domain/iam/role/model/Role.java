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
public class Role extends AuditableDomainModel {

    private UUID roleId;
    private String code;
    private String name;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;
}

