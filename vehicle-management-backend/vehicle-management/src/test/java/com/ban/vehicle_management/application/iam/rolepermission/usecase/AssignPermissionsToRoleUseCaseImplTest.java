package com.ban.vehicle_management.application.iam.rolepermission.usecase;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.AccountAccessInvalidationPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.command.AssignPermissionsToRoleCommand;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionsResult;
import com.ban.vehicle_management.application.iam.rolepermission.port.in.GetRolePermissionsPortIn;
import com.ban.vehicle_management.application.iam.rolepermission.port.out.RolePermissionPortOut;
import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignPermissionsToRoleUseCaseImplTest {

    @Mock RolePortOut rolePortOut;
    @Mock PermissionPortOut permissionPortOut;
    @Mock RolePermissionPortOut rolePermissionPortOut;
    @Mock GetRolePermissionsPortIn getRolePermissionsPortIn;
    @Mock AuditLogPortOut auditLogPortOut;
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock AccountAccessInvalidationPortIn accountAccessInvalidationPortIn;

    @InjectMocks
    AssignPermissionsToRoleUseCaseImpl useCase;

    @Test
    void changedPermissionsInvalidateAllActiveAccountsUsingTheRole() {
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
        RolePermissionsResult result = new RolePermissionsResult(
                roleId, role.getCode(), role.getName(), false, true, List.of(permission));

        when(rolePortOut.findById(roleId)).thenReturn(java.util.Optional.of(role));
        when(permissionPortOut.findByIds(anyCollection())).thenReturn(List.of(permission));
        when(rolePermissionPortOut.findByRoleId(roleId)).thenReturn(List.of());
        when(rolePermissionPortOut.saveAll(anyCollection())).thenAnswer(invocation ->
                List.copyOf(invocation.getArgument(0)));
        when(getRolePermissionsPortIn.getRolePermissions(roleId)).thenReturn(result);

        useCase.assignPermissionsToRole(new AssignPermissionsToRoleCommand(roleId, List.of(permissionId)));

        verify(accountAccessInvalidationPortIn).invalidateActiveAccountsForRole(
                "CUSTOM_OPERATIONS",
                roleId,
                "Quyền của vai trò Custom operations đã được cập nhật."
        );
    }
}
