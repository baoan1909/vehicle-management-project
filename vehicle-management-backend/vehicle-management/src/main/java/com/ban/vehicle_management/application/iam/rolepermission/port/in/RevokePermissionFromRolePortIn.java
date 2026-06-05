package com.ban.vehicle_management.application.iam.rolepermission.port.in;

import com.ban.vehicle_management.application.iam.rolepermission.model.command.RevokePermissionFromRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;

public interface RevokePermissionFromRolePortIn {

    RolePermissionsResult revokePermissionFromRole(RevokePermissionFromRoleCommand command);
}
