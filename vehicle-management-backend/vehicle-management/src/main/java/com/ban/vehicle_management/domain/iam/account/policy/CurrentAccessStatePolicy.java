package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccessState;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.model.RequiredAccessAction;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;

public final class CurrentAccessStatePolicy {

    public Decision resolve(CurrentAccountAccess access, boolean onboardingRequired) {
        if (access == null) {
            return new Decision(CurrentAccessState.ACCOUNT_DISABLED, RequiredAccessAction.CONTACT_ADMIN);
        }
        if (AccountStatus.DISABLED.equals(access.status())) {
            return decision(CurrentAccessState.ACCOUNT_DISABLED, RequiredAccessAction.CONTACT_ADMIN);
        }
        if (AccountStatus.LOCKED.equals(access.status())) {
            return decision(CurrentAccessState.ACCOUNT_LOCKED, RequiredAccessAction.CONTACT_ADMIN);
        }
        if (AccountStatus.PENDING.equals(access.status())) {
            return decision(CurrentAccessState.ACCOUNT_PENDING, RequiredAccessAction.WAIT_FOR_APPROVAL);
        }
        if (!access.roleActive()) {
            return decision(CurrentAccessState.ROLE_DISABLED, RequiredAccessAction.CONTACT_ADMIN);
        }
        if (CustomerApprovalStatus.PENDING.equals(access.customerApprovalStatus())) {
            return decision(CurrentAccessState.CUSTOMER_APPROVAL_REQUIRED, RequiredAccessAction.WAIT_FOR_APPROVAL);
        }
        if (onboardingRequired) {
            return decision(CurrentAccessState.ONBOARDING_REQUIRED, RequiredAccessAction.COMPLETE_PROFILE);
        }
        if (access.customerStatus() != null && !CustomerStatus.ACTIVE.equals(access.customerStatus())) {
            return decision(CurrentAccessState.CUSTOMER_INACTIVE, RequiredAccessAction.CONTACT_ADMIN);
        }
        if (access.employeeStatus() != null && !EmployeeStatus.ACTIVE.equals(access.employeeStatus())) {
            return decision(CurrentAccessState.EMPLOYEE_INACTIVE, RequiredAccessAction.CONTACT_ADMIN);
        }
        if (requiresOrganizationAccess(access)) {
            return decision(CurrentAccessState.ORGANIZATION_ACCESS_REQUIRED,
                    RequiredAccessAction.CONTACT_ORGANIZATION_ADMIN);
        }
        if (access.getEffectivePermissionCodes().isEmpty()) {
            return decision(CurrentAccessState.NO_EFFECTIVE_PERMISSIONS, RequiredAccessAction.CONTACT_ADMIN);
        }
        return decision(CurrentAccessState.ACTIVE, null);
    }

    private boolean requiresOrganizationAccess(CurrentAccountAccess access) {
        boolean partnerScopeAssigned = access.permissionCodes().stream().anyMatch(permission ->
                "PARKING_SCOPE_PARTNER".equals(permission)
                        || "WALLET_SCOPE_PARTNER".equals(permission)
                        || (permission != null && permission.endsWith("_PARTNER")));
        boolean assignedScope = access.permissionCodes().contains("PARKING_SCOPE_ASSIGNED");
        return (partnerScopeAssigned && access.activeOrganizationIds().isEmpty())
                || (assignedScope && access.assignedParkingLotIds().isEmpty());
    }

    private Decision decision(CurrentAccessState state, RequiredAccessAction action) {
        return new Decision(state, action);
    }

    public record Decision(CurrentAccessState state, RequiredAccessAction requiredAction) {
    }
}
