package com.ban.vehicle_management.entrypoint.dto.iam.permission.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PermissionAdminResponse {
    private UUID permissionId;
    private String permissionCode;
    private UUID moduleId;
    private UUID actionId;
    private UUID scopeId;
    private String name;
    private String description;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}
