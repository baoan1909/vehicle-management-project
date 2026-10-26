package com.ban.vehicle_management.domain.iam.account.model;

import static org.junit.jupiter.api.Assertions.*;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CurrentAccountAccessTest {
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    @Test
    void standardAndCustomRolesWithSameStateHaveSameEffectivePermissions() {
        CurrentAccountAccess standard = access("SYSTEM_ADMIN", Set.of("ACCOUNT_READ_ALL"),
                Set.of(), Set.of(), null, null, null, false);
        CurrentAccountAccess custom = access("CUSTOM_PLATFORM", Set.of("ACCOUNT_READ_ALL"),
                Set.of(), Set.of(), null, null, null, false);
        assertEquals(standard.getEffectivePermissionCodes(), custom.getEffectivePermissionCodes());
    }

    @Test
    void inactiveAccountOrRoleHasNoEffectivePermissions() {
        CurrentAccountAccess inactiveRole = new CurrentAccountAccess(
                ACCOUNT_ID, "subject", "user", "user@example.com", ROLE_ID, "CUSTOM", false,
                false, Set.of(), Set.of(), AccountStatus.ACTIVE, null, null, null, false,
                Set.of("ACCOUNT_READ_ALL"));
        CurrentAccountAccess locked = new CurrentAccountAccess(
                ACCOUNT_ID, "subject", "user", "user@example.com", ROLE_ID, "CUSTOM", true,
                false, Set.of(), Set.of(), AccountStatus.LOCKED, null, null, null, false,
                Set.of("ACCOUNT_READ_ALL"));
        assertFalse(inactiveRole.canUseBusinessPermissions());
        assertTrue(inactiveRole.getEffectivePermissionCodes().isEmpty());
        assertTrue(locked.getEffectivePermissionCodes().isEmpty());
    }

    @Test
    void customerLifecycleFiltersOnlyCustomerCapability() {
        CurrentAccountAccess pending = access("CUSTOMER",
                Set.of("ACCOUNT_READ_ALL", "CUSTOMER_VEHICLE_READ_OWN"), Set.of(), Set.of(),
                null, CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING, true);
        assertEquals(Set.of("ACCOUNT_READ_ALL"), pending.getEffectivePermissionCodes());
    }

    @Test
    void accessRevisionIsStableAcrossSetOrderAndIgnoresPresentationIdentityFields() {
        Set<String> first = new LinkedHashSet<>(java.util.List.of("ROLE_READ_ALL", "ACCOUNT_READ_ALL"));
        Set<String> second = new LinkedHashSet<>(java.util.List.of("ACCOUNT_READ_ALL", "ROLE_READ_ALL"));
        CurrentAccountAccess one = access("ROLE_A", first, Set.of(), Set.of(), null, null, null, false);
        CurrentAccountAccess two = new CurrentAccountAccess(
                ACCOUNT_ID, "different-subject", "different-name", "different@example.com", ROLE_ID,
                "ROLE_A", true, false, Set.of(), Set.of(), AccountStatus.ACTIVE, null, null, null,
                false, second);
        assertEquals(one.accessRevision(), two.accessRevision());
    }

    @Test
    void accessRevisionChangesForEveryAuthorizationAndNavigationInput() {
        UUID organizationId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        CurrentAccountAccess base = access("CUSTOM", Set.of("ACCOUNT_READ_ALL"),
                Set.of(), Set.of(), EmployeeStatus.ACTIVE, null, null, false);
        assertNotEquals(base.accessRevision(), copy(base, UUID.randomUUID(), base.roleId(), base.roleCode(),
                base.roleActive(), base.hasActivePartnerOrganizationMembership(), base.status(),
                base.employeeStatus(), base.customerStatus(), base.customerApprovalStatus(),
                base.onboardingRequired(), base.permissionCodes(), base.activeOrganizationIds(),
                base.assignedParkingLotIds()).accessRevision());
        assertNotEquals(base.accessRevision(), copy(base, base.accountId(), UUID.randomUUID(), base.roleCode(),
                base.roleActive(), base.hasActivePartnerOrganizationMembership(), base.status(),
                base.employeeStatus(), base.customerStatus(), base.customerApprovalStatus(),
                base.onboardingRequired(), base.permissionCodes(), base.activeOrganizationIds(),
                base.assignedParkingLotIds()).accessRevision());
        assertNotEquals(base.accessRevision(), copy(base, base.accountId(), base.roleId(), "RENAMED_ROLE",
                base.roleActive(), base.hasActivePartnerOrganizationMembership(), base.status(),
                base.employeeStatus(), base.customerStatus(), base.customerApprovalStatus(),
                base.onboardingRequired(), base.permissionCodes(), base.activeOrganizationIds(),
                base.assignedParkingLotIds()).accessRevision());
        assertNotEquals(base.accessRevision(), copy(base, base.accountId(), base.roleId(), base.roleCode(),
                false, base.hasActivePartnerOrganizationMembership(), base.status(), base.employeeStatus(),
                base.customerStatus(), base.customerApprovalStatus(), base.onboardingRequired(),
                base.permissionCodes(), base.activeOrganizationIds(), base.assignedParkingLotIds()).accessRevision());
        assertNotEquals(base.accessRevision(), copy(base, base.accountId(), base.roleId(), base.roleCode(),
                base.roleActive(), true, base.status(), base.employeeStatus(), base.customerStatus(),
                base.customerApprovalStatus(), base.onboardingRequired(), base.permissionCodes(),
                base.activeOrganizationIds(), base.assignedParkingLotIds()).accessRevision());
        assertNotEquals(base.accessRevision(), copy(base, base.accountId(), base.roleId(), base.roleCode(),
                base.roleActive(), base.hasActivePartnerOrganizationMembership(), AccountStatus.LOCKED,
                base.employeeStatus(), base.customerStatus(), base.customerApprovalStatus(),
                base.onboardingRequired(), base.permissionCodes(), base.activeOrganizationIds(),
                base.assignedParkingLotIds()).accessRevision());
        assertNotEquals(base.accessRevision(), access("CUSTOM", Set.of("ROLE_READ_ALL"),
                Set.of(), Set.of(), EmployeeStatus.ACTIVE, null, null, false).accessRevision());
        assertNotEquals(base.accessRevision(), access("CUSTOM", Set.of("ACCOUNT_READ_ALL"),
                Set.of(), Set.of(), EmployeeStatus.INACTIVE, null, null, false).accessRevision());
        assertNotEquals(base.accessRevision(), access("CUSTOM", Set.of("ACCOUNT_READ_ALL"),
                Set.of(), Set.of(), EmployeeStatus.ACTIVE, CustomerStatus.ACTIVE,
                null, false).accessRevision());
        assertNotEquals(base.accessRevision(), access("CUSTOM", Set.of("ACCOUNT_READ_ALL"),
                Set.of(), Set.of(), EmployeeStatus.ACTIVE, null,
                CustomerApprovalStatus.PENDING, false).accessRevision());
        assertNotEquals(base.accessRevision(), access("CUSTOM", Set.of("ACCOUNT_READ_ALL"),
                Set.of(organizationId), Set.of(), EmployeeStatus.ACTIVE, null, null, false).accessRevision());
        assertNotEquals(base.accessRevision(), access("CUSTOM", Set.of("ACCOUNT_READ_ALL"),
                Set.of(), Set.of(parkingLotId), EmployeeStatus.ACTIVE, null, null, false).accessRevision());
        assertNotEquals(base.accessRevision(), access("CUSTOM", Set.of("ACCOUNT_READ_ALL"),
                Set.of(), Set.of(), EmployeeStatus.ACTIVE, null, null, true).accessRevision());
    }

    private CurrentAccountAccess copy(
            CurrentAccountAccess source,
            UUID accountId,
            UUID roleId,
            String roleCode,
            boolean roleActive,
            boolean partnerMembership,
            AccountStatus status,
            EmployeeStatus employeeStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus customerApprovalStatus,
            boolean onboardingRequired,
            Set<String> permissions,
            Set<UUID> organizationIds,
            Set<UUID> parkingLotIds
    ) {
        return new CurrentAccountAccess(accountId, source.subject(), source.username(), source.email(), roleId,
                roleCode, roleActive, partnerMembership, organizationIds, parkingLotIds, status, employeeStatus,
                customerStatus, customerApprovalStatus, onboardingRequired, permissions);
    }

    private CurrentAccountAccess access(
            String roleCode, Set<String> permissions, Set<UUID> organizationIds, Set<UUID> lotIds,
            EmployeeStatus employeeStatus, CustomerStatus customerStatus,
            CustomerApprovalStatus approvalStatus, boolean onboardingRequired) {
        return new CurrentAccountAccess(
                ACCOUNT_ID, "subject", "user", "user@example.com", ROLE_ID, roleCode, true,
                !organizationIds.isEmpty(), organizationIds, lotIds, AccountStatus.ACTIVE,
                employeeStatus, customerStatus, approvalStatus, onboardingRequired, permissions);
    }
}
