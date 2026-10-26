package com.ban.vehicle_management.application.iam.rolepermission.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.AccountAccessInvalidationPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.RevokePermissionFromRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionsPortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.out.RolePermissionPortOut;
import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.domain.iam.role.model.RolePermission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevokePermissionFromRolePortInImplTest {

    @Mock RolePortOut rolePortOut;
    @Mock PermissionPortOut permissionPortOut;
    @Mock RolePermissionPortOut rolePermissionPortOut;
    @Mock GetRolePermissionsPortIn getRolePermissionsPortIn;
    @Mock AuditLogPortOut auditLogPortOut;
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock AccountAccessInvalidationPortIn accountAccessInvalidationPortIn;

    @InjectMocks
    RevokePermissionFromRolePortInImpl useCase;

    @Test
    void revokedPermissionInvalidatesAllActiveAccountsUsingTheRole() {
        UUID roleId = UUID.randomUUID();
        UUID permissionId = UUID.randomUUID();
        Role role = new Role();
        role.setRoleId(roleId);
        role.setCode("CUSTOM_OPERATIONS");
        role.setName("Custom operations");
        role.setIsSystem(false);
        Permission permission = new Permission();
        permission.setPermissionId(permissionId);
        permission.setPermissionCode("DASHBOARD_READ_ALL");
        RolePermission rolePermission = new RolePermission(
                UUID.randomUUID(), roleId, permissionId, true, false
        );
        RolePermissionsResult result = new RolePermissionsResult(
                roleId, role.getCode(), role.getName(), false, true, List.of()
        );

        when(rolePortOut.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionPortOut.findById(permissionId)).thenReturn(Optional.of(permission));
        when(rolePermissionPortOut.findByRoleIdAndPermissionId(roleId, permissionId))
                .thenReturn(Optional.of(rolePermission));
        when(getRolePermissionsPortIn.getRolePermissions(roleId)).thenReturn(result);

        useCase.revokePermissionFromRole(new RevokePermissionFromRoleCommand(roleId, permissionId));

        verify(rolePermissionPortOut).saveAll(List.of(rolePermission));
        verify(accountAccessInvalidationPortIn).invalidateActiveAccountsForRole(
                "CUSTOM_OPERATIONS",
                roleId,
                "Quyền của vai trò Custom operations đã được cập nhật."
        );
    }
}
