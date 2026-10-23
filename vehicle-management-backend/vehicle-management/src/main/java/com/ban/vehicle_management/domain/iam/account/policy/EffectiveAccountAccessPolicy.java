package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class EffectiveAccountAccessPolicy {

    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("[A-Z0-9_-]+");

    public boolean canUseBusinessPermissions(AccessState state) {
        if (state == null
                || !state.roleActive()
                || !AccountStatus.ACTIVE.equals(state.accountStatus())
                || !isValidRoleCode(state.roleCode())) {
            return false;
        }

        AdminProvisionableAccountRoleCode role = resolveRole(state.roleCode());
        if (role == null) {
            return canUseCustomRolePermissions(state);
        }

        return switch (role) {
            case SYSTEM_ADMIN, PARTNER_ADMIN -> true;
            case PARKING_MANAGER, EMPLOYEE -> EmployeeStatus.ACTIVE.equals(state.employeeStatus());
            case CUSTOMER -> CustomerStatus.ACTIVE.equals(state.customerStatus())
                    && CustomerApprovalStatus.APPROVED.equals(state.customerApprovalStatus());
        };
    }

    private boolean canUseCustomRolePermissions(AccessState state) {
        if (state.employeeStatus() != null
                && !EmployeeStatus.ACTIVE.equals(state.employeeStatus())) {
            return false;
        }

        boolean hasCustomerLifecycle = state.customerStatus() != null
                || state.customerApprovalStatus() != null;
        return !hasCustomerLifecycle
                || (CustomerStatus.ACTIVE.equals(state.customerStatus())
                && CustomerApprovalStatus.APPROVED.equals(state.customerApprovalStatus()));
    }

    public Set<String> resolveEffectivePermissionCodes(
            AccessState state,
            Collection<String> assignedPermissionCodes
    ) {
        if (!canUseBusinessPermissions(state)
                || assignedPermissionCodes == null
                || assignedPermissionCodes.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> deterministicCodes = assignedPermissionCodes.stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(deterministicCodes);
    }

    private AdminProvisionableAccountRoleCode resolveRole(String roleCode) {
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isValidRoleCode(String roleCode) {
        return roleCode != null && ROLE_CODE_PATTERN.matcher(roleCode).matches();
    }

    public record AccessState(
            String roleCode,
            boolean roleActive,
            AccountStatus accountStatus,
            EmployeeStatus employeeStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus customerApprovalStatus
    ) {
    }
}
