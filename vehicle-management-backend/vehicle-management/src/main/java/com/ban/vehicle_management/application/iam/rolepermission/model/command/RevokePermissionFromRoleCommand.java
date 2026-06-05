package com.ban.vehicle_management.application.iam.rolepermission.model.command;

import java.util.UUID;

public record RevokePermissionFromRoleCommand(
        UUID roleId,
        UUID permissionId
) {
}
