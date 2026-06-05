package com.ban.vehicle_management.application.iam.rolepermission.port.in;

import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;

import java.util.UUID;

public interface GetRolePermissionsPortIn {

    RolePermissionsResult getRolePermissions(UUID roleId);
}
