package com.ban.vehicle_management.application.iam.rolepermission.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionAuditLogResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionAuditLogsPortIn;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetRolePermissionAuditLogsUseCaseImpl implements GetRolePermissionAuditLogsPortIn {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final RolePortOut rolePortOut;
    private final AuditLogPortOut auditLogPortOut;

    public GetRolePermissionAuditLogsUseCaseImpl(RolePortOut rolePortOut, AuditLogPortOut auditLogPortOut) {
        this.rolePortOut = rolePortOut;
        this.auditLogPortOut = auditLogPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolePermissionAuditLogResult> getRolePermissionAuditLogs(UUID roleId, Integer limit) {
        rolePortOut.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found"));

        return auditLogPortOut.findRolePermissionAuditLogs(roleId, normalizeLimit(limit));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        if (limit < 1) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }
}
