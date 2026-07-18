package com.ban.vehicle_management.application.audit.auditlog.port.out;

import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionAuditLogResult;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogPortOut {

    AuditLog save(AuditLog auditLog);

    List<RolePermissionAuditLogResult> findRolePermissionAuditLogs(UUID roleId, int limit);
}
