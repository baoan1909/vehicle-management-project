package com.ban.vehicle_management.application.iam.rolepermission.port.in;

import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionAuditLogResult;

import java.util.List;
import java.util.UUID;

public interface GetRolePermissionAuditLogsPortIn {

    List<RolePermissionAuditLogResult> getRolePermissionAuditLogs(UUID roleId, Integer limit);
}
