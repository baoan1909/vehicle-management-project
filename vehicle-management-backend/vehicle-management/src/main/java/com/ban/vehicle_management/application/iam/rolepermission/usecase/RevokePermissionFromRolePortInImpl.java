package com.ban.vehicle_management.application.iam.rolepermission.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.RevokePermissionFromRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionsPortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.RevokePermissionFromRolePortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.out.RolePermissionPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.domain.iam.role.model.RolePermission;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RevokePermissionFromRolePortInImpl implements RevokePermissionFromRolePortIn {

    private static final String AUDIT_ACTION_REVOKE = "ROLE_PERMISSION_REVOKE";

    private final RolePortOut rolePortOut;
    private final PermissionPortOut permissionPortOut;
    private final RolePermissionPortOut rolePermissionPortOut;
    private final GetRolePermissionsPortIn getRolePermissionsPortIn;
    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;

    public RevokePermissionFromRolePortInImpl(
            RolePortOut rolePortOut,
            PermissionPortOut permissionPortOut,
            RolePermissionPortOut rolePermissionPortOut,
            GetRolePermissionsPortIn getRolePermissionsPortIn,
            AuditLogPortOut auditLogPortOut,
            CurrentAccountPortIn currentAccountPortIn
    ) {
        this.rolePortOut = rolePortOut;
        this.permissionPortOut = permissionPortOut;
        this.rolePermissionPortOut = rolePermissionPortOut;
        this.getRolePermissionsPortIn = getRolePermissionsPortIn;
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    @Override
    @Transactional
    public RolePermissionsResult revokePermissionFromRole(RevokePermissionFromRoleCommand command) {
        Role role = rolePortOut.findById(command.roleId())
                .orElseThrow(() -> new NotFoundException("Role not found"));
        ensureRoleCanBeManaged(role);

        Permission permission = permissionPortOut.findById(command.permissionId())
                .orElseThrow(() -> new NotFoundException("Permission not found"));

        boolean revoked = rolePermissionPortOut.findByRoleIdAndPermissionId(role.getRoleId(), command.permissionId())
                .map(this::deactivateIfAllowed)
                .orElse(false);

        if (revoked) {
            writeRevokeAuditLog(role, permission);
        }

        return getRolePermissionsPortIn.getRolePermissions(role.getRoleId());
    }

    private void ensureRoleCanBeManaged(Role role) {
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BadRequestException("System role permissions cannot be modified");
        }
    }

    private boolean deactivateIfAllowed(RolePermission rolePermission) {
        if (Boolean.TRUE.equals(rolePermission.getIsSystem())) {
            throw new BadRequestException("System role permission cannot be revoked");
        }

        if (Boolean.FALSE.equals(rolePermission.getIsActive())) {
            return false;
        }

        rolePermission.setIsActive(false);
        rolePermissionPortOut.saveAll(List.of(rolePermission));
        return true;
    }

    private void writeRevokeAuditLog(Role role, Permission permission) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountId().orElse(null));
        auditLog.setAction(AUDIT_ACTION_REVOKE);
        auditLog.setTargetSchema("iam");
        auditLog.setTargetTable("role_permissions");
        auditLog.setTargetId(role.getRoleId());
        auditLog.setOldData(Map.of(
                "roleCode", role.getCode(),
                "permissionCode", permission.getPermissionCode(),
                "active", true
        ));
        auditLog.setNewData(Map.of(
                "roleCode", role.getCode(),
                "permissionCode", permission.getPermissionCode(),
                "active", false,
                "removedPermissionCodes", List.of(permission.getPermissionCode())
        ));

        auditLogPortOut.save(auditLog);
    }
}
