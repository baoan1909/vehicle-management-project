package com.ban.vehicle_management.application.iam.rolepermission.model.result;

import com.ban.vehicle_management.domain.iam.permission.model.Permission;

import java.util.List;
import java.util.UUID;

public record RolePermissionsResult(
        UUID roleId,
        String roleCode,
        String roleName,
        Boolean isSystem,
        Boolean isActive,
        List<Permission> permissions
) {
}
