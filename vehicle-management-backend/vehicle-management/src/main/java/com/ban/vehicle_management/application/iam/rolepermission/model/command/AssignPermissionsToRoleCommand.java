package com.ban.vehicle_management.application.iam.rolepermission.model.command;

import java.util.List;
import java.util.UUID;

public record AssignPermissionsToRoleCommand(
        UUID roleId,
        List<UUID> permissionIds
) {
}
