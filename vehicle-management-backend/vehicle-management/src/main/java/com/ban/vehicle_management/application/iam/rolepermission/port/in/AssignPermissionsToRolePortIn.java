package com.ban.vehicle_management.application.iam.rolepermission.port.in;

import com.ban.vehicle_management.application.iam.rolepermission.model.command.AssignPermissionsToRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;

public interface AssignPermissionsToRolePortIn {

    RolePermissionsResult assignPermissionsToRole(AssignPermissionsToRoleCommand command);
}
