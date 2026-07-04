package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class AccountOnboardingPolicy {

    public boolean isOnboardingRequired(
            AccountProfileState state,
            boolean latestSystemAdminApprovalRequestExists
    ) {
        requireState(state);
        AdminProvisionableAccountRoleCode roleCode = resolveProvisionableRole(state.roleCode());
        if (roleCode == null) {
            return state.userProfileId() == null;
        }
        if (requiresEmployeeRecord(roleCode)) {
            return state.userProfileId() == null || state.employeeId() == null;
        }
        if (AdminProvisionableAccountRoleCode.CUSTOMER.equals(roleCode)) {
            return state.userProfileId() == null || state.customerId() == null;
        }
        if (AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.equals(roleCode)) {
            return state.userProfileId() == null
                    || isSystemAdminApprovalRequired(state, latestSystemAdminApprovalRequestExists);
        }
        return state.userProfileId() == null;
    }

    public boolean needsSystemAdminApprovalLookup(AccountProfileState state) {
        requireState(state);
        return state.userProfileId() != null
                && AccountStatus.PENDING.equals(state.accountStatus())
                && AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.equals(resolveProvisionableRole(state.roleCode()));
    }

    public boolean requiresEmployeeRecord(AdminProvisionableAccountRoleCode roleCode) {
        return roleCode != null && roleCode.requiresEmployeeRecord();
    }

    public boolean shouldCreateSystemAdminApproval(AccountProfileState state) {
        requireState(state);
        return AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.equals(resolveProvisionableRole(state.roleCode()))
                && AccountStatus.PENDING.equals(state.accountStatus());
    }

    public String defaultJobTitle(AdminProvisionableAccountRoleCode roleCode) {
        return switch (requireSupportedOnboardingRole(roleCode)) {
            case EMPLOYEE -> "Parking Staff";
            case PARKING_MANAGER -> "Parking Manager";
            case CUSTOMER, SYSTEM_ADMIN -> null;
        };
    }

    public AdminProvisionableAccountRoleCode requireSupportedOnboardingRole(String roleCode) {
        AdminProvisionableAccountRoleCode resolvedRole = resolveProvisionableRole(roleCode);
        if (resolvedRole == null) {
            throw new BadRequestException("Current account role is not supported for onboarding");
        }
        return resolvedRole;
    }

    public AdminProvisionableAccountRoleCode resolveProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isSystemAdminApprovalRequired(
            AccountProfileState state,
            boolean latestSystemAdminApprovalRequestExists
    ) {
        if (!AccountStatus.PENDING.equals(state.accountStatus())) {
            return false;
        }
        return !latestSystemAdminApprovalRequestExists;
    }

    private AdminProvisionableAccountRoleCode requireSupportedOnboardingRole(
            AdminProvisionableAccountRoleCode roleCode
    ) {
        if (roleCode == null) {
            throw new BadRequestException("Current account role is not supported for onboarding");
        }
        return roleCode;
    }

    private void requireState(AccountProfileState state) {
        if (state == null) {
            throw new BadRequestException("accountProfileState must not be null");
        }
    }
}
