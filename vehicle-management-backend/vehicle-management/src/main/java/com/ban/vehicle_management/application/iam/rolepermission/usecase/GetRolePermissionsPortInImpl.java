package com.ban.vehicle_management.application.iam.rolepermission.usecase;

import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionsPortIn;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetRolePermissionsPortInImpl implements GetRolePermissionsPortIn {

    private final RolePortOut rolePortOut;
    private final PermissionPortOut permissionPortOut;

    public GetRolePermissionsPortInImpl(
            RolePortOut rolePortOut,
            PermissionPortOut permissionPortOut
    ) {
        this.rolePortOut = rolePortOut;
        this.permissionPortOut = permissionPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionsResult getRolePermissions(UUID roleId) {
        Role role = rolePortOut.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found"));

        return new RolePermissionsResult(
                role.getRoleId(),
                role.getCode(),
                role.getName(),
                role.getIsSystem(),
                role.getIsActive(),
                permissionPortOut.findByRoleId(roleId)
        );
    }
}
