package com.ban.vehicle_management.application.iam.rolepermission.usecase;

import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.RevokePermissionFromRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionsPortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.RevokePermissionFromRolePortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.out.RolePermissionPortOut;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.domain.iam.role.model.RolePermission;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RevokePermissionFromRolePortInImpl implements RevokePermissionFromRolePortIn {

    private final RolePortOut rolePortOut;
    private final PermissionPortOut permissionPortOut;
    private final RolePermissionPortOut rolePermissionPortOut;
    private final GetRolePermissionsPortIn getRolePermissionsPortIn;

    public RevokePermissionFromRolePortInImpl(
            RolePortOut rolePortOut,
            PermissionPortOut permissionPortOut,
            RolePermissionPortOut rolePermissionPortOut,
            GetRolePermissionsPortIn getRolePermissionsPortIn
    ) {
        this.rolePortOut = rolePortOut;
        this.permissionPortOut = permissionPortOut;
        this.rolePermissionPortOut = rolePermissionPortOut;
        this.getRolePermissionsPortIn = getRolePermissionsPortIn;
    }

    @Override
    @Transactional
    public RolePermissionsResult revokePermissionFromRole(RevokePermissionFromRoleCommand command) {
        Role role = rolePortOut.findById(command.roleId())
                .orElseThrow(() -> new NotFoundException("Role not found"));
        ensureRoleCanBeManaged(role);

        permissionPortOut.findById(command.permissionId())
                .orElseThrow(() -> new NotFoundException("Permission not found"));

        rolePermissionPortOut.findByRoleIdAndPermissionId(role.getRoleId(), command.permissionId())
                .ifPresent(this::deactivateIfAllowed);

        return getRolePermissionsPortIn.getRolePermissions(role.getRoleId());
    }

    private void ensureRoleCanBeManaged(Role role) {
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BadRequestException("System role permissions cannot be modified");
        }
    }

    private void deactivateIfAllowed(RolePermission rolePermission) {
        if (Boolean.TRUE.equals(rolePermission.getIsSystem())) {
            throw new BadRequestException("System role permission cannot be revoked");
        }

        if (Boolean.FALSE.equals(rolePermission.getIsActive())) {
            return;
        }

        rolePermission.setIsActive(false);
        rolePermissionPortOut.saveAll(List.of(rolePermission));
    }
}
