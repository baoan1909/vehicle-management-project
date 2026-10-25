package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.*;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EffectiveAccountAccessPolicyTest {
    private final EffectiveAccountAccessPolicy policy = new EffectiveAccountAccessPolicy();

    @Test
    void commonGatesDependOnlyOnAccountAndRoleActivation() {
        assertTrue(policy.canUseBusinessPermissions(state("ANY_ROLE", true, AccountStatus.ACTIVE)));
        assertTrue(policy.canUseBusinessPermissions(state("CUSTOM_ROLE", true, AccountStatus.ACTIVE)));
        assertFalse(policy.canUseBusinessPermissions(state("SYSTEM_ADMIN", false, AccountStatus.ACTIVE)));
        assertFalse(policy.canUseBusinessPermissions(state("CUSTOM_ROLE", true, AccountStatus.LOCKED)));
        assertFalse(policy.canUseBusinessPermissions(null));
    }

    @Test
    void roleNameDoesNotChangeAuthorizationResult() {
        Set<String> permissions = Set.of("ACCOUNT_READ_ALL");
        assertEquals(
                policy.resolveEffectivePermissionCodes(state("SYSTEM_ADMIN", true, AccountStatus.ACTIVE), permissions),
                policy.resolveEffectivePermissionCodes(state("CUSTOM_PLATFORM_ROLE", true, AccountStatus.ACTIVE), permissions));
    }

    @Test
    void customerOwnPermissionRequiresActiveApprovedCustomerButUnrelatedPermissionDoesNot() {
        var pendingCustomer = new EffectiveAccountAccessPolicy.AccessState(
                "CUSTOM_ROLE", true, false, Set.of(), Set.of(), AccountStatus.ACTIVE, null,
                CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING);
        assertEquals(Set.of("ACCOUNT_READ_ALL"), policy.resolveEffectivePermissionCodes(
                pendingCustomer, Set.of("ACCOUNT_READ_ALL", "CUSTOMER_VEHICLE_READ_OWN")));
    }

    @Test
    void assignedScopeRequiresActiveEmployeeAndAtLeastOneAssignment() {
        UUID lotId = UUID.randomUUID();
        var activeAssigned = new EffectiveAccountAccessPolicy.AccessState(
                "CUSTOM_OPERATOR", true, false, Set.of(), Set.of(lotId), AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE, null, null);
        var inactiveAssigned = new EffectiveAccountAccessPolicy.AccessState(
                "EMPLOYEE", true, false, Set.of(), Set.of(lotId), AccountStatus.ACTIVE,
                EmployeeStatus.INACTIVE, null, null);
        assertEquals(Set.of("PARKING_SCOPE_ASSIGNED"), policy.resolveEffectivePermissionCodes(
                activeAssigned, Set.of("PARKING_SCOPE_ASSIGNED")));
        assertTrue(policy.resolveEffectivePermissionCodes(
                inactiveAssigned, Set.of("PARKING_SCOPE_ASSIGNED")).isEmpty());
    }

    @Test
    void partnerScopeRequiresActiveOrganizationMembership() {
        UUID organizationId = UUID.randomUUID();
        var activeMembership = new EffectiveAccountAccessPolicy.AccessState(
                "CUSTOM_PARTNER", true, true, Set.of(organizationId), Set.of(),
                AccountStatus.ACTIVE, null, null, null);
        var noMembership = new EffectiveAccountAccessPolicy.AccessState(
                "PARTNER_ADMIN", true, false, Set.of(), Set.of(),
                AccountStatus.ACTIVE, null, null, null);
        assertEquals(Set.of("PARKING_SCOPE_PARTNER"), policy.resolveEffectivePermissionCodes(
                activeMembership, Set.of("PARKING_SCOPE_PARTNER")));
        assertTrue(policy.resolveEffectivePermissionCodes(
                noMembership, Set.of("PARKING_SCOPE_PARTNER")).isEmpty());
    }

    @Test
    void malformedPermissionsAreRemovedAndOutputIsImmutable() {
        Set<String> effective = policy.resolveEffectivePermissionCodes(
                state("CUSTOM", true, AccountStatus.ACTIVE),
                java.util.Arrays.asList(" account_read_all ", null, "", "bad permission"));
        assertEquals(Set.of("ACCOUNT_READ_ALL"), effective);
        assertThrows(UnsupportedOperationException.class, () -> effective.add("ROLE_READ_ALL"));
    }

    private EffectiveAccountAccessPolicy.AccessState state(
            String roleCode, boolean roleActive, AccountStatus accountStatus) {
        return new EffectiveAccountAccessPolicy.AccessState(
                roleCode, roleActive, false, Set.of(), Set.of(), accountStatus, null, null, null);
    }
}
