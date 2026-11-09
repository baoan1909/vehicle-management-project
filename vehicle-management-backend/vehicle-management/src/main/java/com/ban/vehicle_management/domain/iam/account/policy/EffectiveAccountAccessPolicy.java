package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Resolves effective permissions without granting or denying from a role name. */
public final class EffectiveAccountAccessPolicy {

    private static final Set<String> ACCOUNT_DELETION_LIFECYCLE_PERMISSIONS = Set.of(
            "ACCOUNT_DELETION_REQUEST_SELF",
            "ACCOUNT_DELETION_READ_SELF",
            "ACCOUNT_DELETION_CANCEL_SELF"
    );

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
                && AccountStatus.PENDING_DELETION.equals(state.accountStatus()))
                || (state != null
                && state.roleActive()
                && AccountStatus.PENDING.equals(state.accountStatus())
                && (state.customerApprovalStatus() == null
                || CustomerApprovalStatus.PENDING.equals(state.customerApprovalStatus())
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
        if (AccountStatus.PENDING_DELETION.equals(state.accountStatus())) {
            return normalizedAssignedPermissions.stream()
                    .filter(ACCOUNT_DELETION_LIFECYCLE_PERMISSIONS::contains)
                    .sorted()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            Collections::unmodifiableSet));
        }
        if (AccountStatus.PENDING.equals(state.accountStatus())
                && !isCustomerPreApprovalSupportState(state)) {
            return lifecycleDeletionPermissions(normalizedAssignedPermissions);
        }
        if (mustRestrictToLifecycleSafePermissions(state, normalizedAssignedPermissions)) {
            if (!isCustomerPreApprovalSupportState(state)) {
                return lifecycleDeletionPermissions(normalizedAssignedPermissions);
            }
            return normalizedAssignedPermissions.stream()
                    .filter(permissionCode -> ACCOUNT_DELETION_LIFECYCLE_PERMISSIONS.contains(permissionCode)
                            || PermissionAccessPrerequisitePolicy.PermissionPrerequisite.CUSTOMER_SUPPORT
                            .equals(prerequisitePolicy.classificationFor(permissionCode)))
                    .filter(permissionCode -> prerequisitePolicy.isSatisfied(permissionCode, state))
                    .sorted()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            Collections::unmodifiableSet));
        }
        LinkedHashSet<String> effectivePermissions = normalizedAssignedPermissions.stream()
                .filter(permissionCode -> canUseBusinessPermissions(state)
                        || PermissionAccessPrerequisitePolicy.PermissionPrerequisite.CUSTOMER_SUPPORT.equals(
                        prerequisitePolicy.classificationFor(permissionCode)))
                .filter(permissionCode -> prerequisitePolicy.isSatisfied(permissionCode, state))
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(effectivePermissions);
    }

    private Set<String> lifecycleDeletionPermissions(Set<String> assignedPermissions) {
        return assignedPermissions.stream()
                .filter(ACCOUNT_DELETION_LIFECYCLE_PERMISSIONS::contains)
                .sorted()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        Collections::unmodifiableSet));
    }

    private boolean isCustomerPreApprovalSupportState(AccessState state) {
        return state.employeeStatus() == null
                && state.customerStatus() != null
                && (CustomerApprovalStatus.PENDING.equals(state.customerApprovalStatus())
                || CustomerApprovalStatus.REJECTED.equals(state.customerApprovalStatus()));
    }

    private boolean mustRestrictToLifecycleSafePermissions(
            AccessState state,
            Set<String> assignedPermissionCodes
    ) {
        if (state.onboardingRequired()
                || ApprovalRequestStatus.PENDING.equals(state.onboardingApprovalStatus())
                || ApprovalRequestStatus.REJECTED.equals(state.onboardingApprovalStatus())
                || ApprovalRequestStatus.CANCELLED.equals(state.onboardingApprovalStatus())) {
            return true;
        }
        if (state.employeeStatus() != null && !EmployeeStatus.ACTIVE.equals(state.employeeStatus())) {
            return true;
        }
        if (state.customerStatus() != null && !CustomerStatus.ACTIVE.equals(state.customerStatus())) {
            return true;
        }
        if (state.customerApprovalStatus() != null
                && !CustomerApprovalStatus.APPROVED.equals(state.customerApprovalStatus())) {
            return true;
        }
        boolean requiresPartnerScope = assignedPermissionCodes.stream().anyMatch(permissionCode ->
                PermissionAccessPrerequisitePolicy.PermissionPrerequisite.PARTNER_ORGANIZATION
                        .equals(prerequisitePolicy.classificationFor(permissionCode)));
        if (requiresPartnerScope && state.activeOrganizationIds().isEmpty()) {
            return true;
        }
        return assignedPermissionCodes.contains("PARKING_SCOPE_ASSIGNED")
                && state.assignedParkingLotIds().isEmpty();
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
            CustomerApprovalStatus customerApprovalStatus,
            boolean onboardingRequired,
            ApprovalRequestStatus onboardingApprovalStatus
    ) {
        public AccessState {
            activeOrganizationIds = activeOrganizationIds == null ? Set.of() : Set.copyOf(activeOrganizationIds);
            assignedParkingLotIds = assignedParkingLotIds == null ? Set.of() : Set.copyOf(assignedParkingLotIds);
        }

        public AccessState(
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
            this(roleCode, roleActive, hasActivePartnerOrganizationMembership, activeOrganizationIds,
                    assignedParkingLotIds, accountStatus, employeeStatus, customerStatus,
                    customerApprovalStatus, false, null);
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
                    accountStatus, employeeStatus, customerStatus, customerApprovalStatus, false, null);
        }
    }
}
