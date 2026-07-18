package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;

import java.util.Set;
import java.util.UUID;

public record CurrentAccountAccess(
        UUID accountId,
        String subject,
        String username,
        String email,
        UUID roleId,
        String roleCode,
        AccountStatus status,
        EmployeeStatus employeeStatus,
        Set<String> permissionCodes
) {
    public boolean canUseBusinessPermissions() {
        if (!AccountStatus.ACTIVE.equals(status)) {
            return false;
        }

        AdminProvisionableAccountRoleCode provisionableRole = resolveProvisionableRole(roleCode);
        if (provisionableRole == null || !provisionableRole.requiresEmployeeRecord()) {
            return true;
        }

        return EmployeeStatus.ACTIVE.equals(employeeStatus);
    }

    public Set<String> getEffectivePermissionCodes() {
        return canUseBusinessPermissions() ? permissionCodes : Set.of();
    }

    private static AdminProvisionableAccountRoleCode resolveProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
