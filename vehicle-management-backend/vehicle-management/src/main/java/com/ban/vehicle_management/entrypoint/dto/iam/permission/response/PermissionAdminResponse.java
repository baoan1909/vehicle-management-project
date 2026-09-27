package com.ban.vehicle_management.entrypoint.dto.iam.permission.response;

import java.time.Instant;

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
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
