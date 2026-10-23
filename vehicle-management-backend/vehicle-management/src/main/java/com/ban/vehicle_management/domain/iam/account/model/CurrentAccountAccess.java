package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.domain.iam.account.policy.EffectiveAccountAccessPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
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
        boolean roleActive,
        AccountStatus status,
        EmployeeStatus employeeStatus,
        CustomerStatus customerStatus,
        CustomerApprovalStatus customerApprovalStatus,
        Set<String> permissionCodes
) {
    private static final EffectiveAccountAccessPolicy EFFECTIVE_ACCESS_POLICY =
            new EffectiveAccountAccessPolicy();

    public CurrentAccountAccess(
            UUID accountId,
            String subject,
            String username,
            String email,
            UUID roleId,
            String roleCode,
            AccountStatus status,
            EmployeeStatus employeeStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus customerApprovalStatus,
            Set<String> permissionCodes
    ) {
        this(
                accountId,
                subject,
                username,
                email,
                roleId,
                roleCode,
                true,
                status,
                employeeStatus,
                customerStatus,
                customerApprovalStatus,
                permissionCodes
        );
    }

    public CurrentAccountAccess(
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
        this(accountId, subject, username, email, roleId, roleCode, true, status, employeeStatus, null, null, permissionCodes);
    }

    public boolean canUseBusinessPermissions() {
        return EFFECTIVE_ACCESS_POLICY.canUseBusinessPermissions(toAccessState());
    }

    public Set<String> getEffectivePermissionCodes() {
        return EFFECTIVE_ACCESS_POLICY.resolveEffectivePermissionCodes(toAccessState(), permissionCodes);
    }

    private EffectiveAccountAccessPolicy.AccessState toAccessState() {
        return new EffectiveAccountAccessPolicy.AccessState(
                roleCode,
                roleActive,
                status,
                employeeStatus,
                customerStatus,
                customerApprovalStatus
        );
    }
}
