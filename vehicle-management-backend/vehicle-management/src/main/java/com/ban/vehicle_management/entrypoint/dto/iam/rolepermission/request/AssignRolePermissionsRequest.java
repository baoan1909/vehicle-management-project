package com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.request;

import java.util.List;
import java.util.UUID;

public record AssignRolePermissionsRequest(
        List<UUID> permissionIds
) {
}
