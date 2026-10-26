package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Resolves effective permissions without granting or denying from a role name. */
public final class EffectiveAccountAccessPolicy {

    private final PermissionAccessPrerequisitePolicy prerequisitePolicy =
            new PermissionAccessPrerequisitePolicy();

    public boolean canUseBusinessPermissions(AccessState state) {
        return state != null
                && state.roleActive()
                && AccountStatus.ACTIVE.equals(state.accountStatus());
    }

    /** Allows a pending customer account to use only lifecycle-filtered support permissions. */
    public boolean canUseEffectivePermissions(AccessState state) {
        return canUseBusinessPermissions(state)
                || (state != null
                && state.roleActive()
                && AccountStatus.PENDING.equals(state.accountStatus())
                && (CustomerApprovalStatus.PENDING.equals(state.customerApprovalStatus())
                || CustomerApprovalStatus.REJECTED.equals(state.customerApprovalStatus())));
    }

    public Set<String> resolveEffectivePermissionCodes(
            AccessState state,
            Collection<String> assignedPermissionCodes
    ) {
        if (!canUseEffectivePermissions(state)
                || assignedPermissionCodes == null
                || assignedPermissionCodes.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedAssignedPermissions = prerequisitePolicy
                .normalizeAssignedPermissions(assignedPermissionCodes);
        LinkedHashSet<String> effectivePermissions = normalizedAssignedPermissions.stream()
                .filter(permissionCode -> canUseBusinessPermissions(state)
                        || PermissionAccessPrerequisitePolicy.PermissionPrerequisite.CUSTOMER_SUPPORT.equals(
                        prerequisitePolicy.classificationFor(permissionCode)))
                .filter(permissionCode -> prerequisitePolicy.isSatisfied(permissionCode, state))
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(effectivePermissions);
    }

    public record AccessState(
            String roleCode,
            boolean roleActive,
            boolean hasActivePartnerOrganizationMembership,
            Set<UUID> activeOrganizationIds,
            Set<UUID> assignedParkingLotIds,
            AccountStatus accountStatus,
            EmployeeStatus employeeStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus customerApprovalStatus
    ) {
        public AccessState {
            activeOrganizationIds = activeOrganizationIds == null ? Set.of() : Set.copyOf(activeOrganizationIds);
            assignedParkingLotIds = assignedParkingLotIds == null ? Set.of() : Set.copyOf(assignedParkingLotIds);
        }

        public AccessState(
                String roleCode,
                boolean roleActive,
                boolean hasActivePartnerOrganizationMembership,
                AccountStatus accountStatus,
                EmployeeStatus employeeStatus,
                CustomerStatus customerStatus,
                CustomerApprovalStatus customerApprovalStatus
        ) {
            this(roleCode, roleActive, hasActivePartnerOrganizationMembership, Set.of(), Set.of(),
                    accountStatus, employeeStatus, customerStatus, customerApprovalStatus);
        }
    }
}
