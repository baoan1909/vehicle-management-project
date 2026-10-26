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
    void pendingCustomerReceivesOnlyExplicitSupportPermissions() {
        Set<String> assigned = Set.of(
                "SUPPORT_WIDGET_ACCESS_OWN",
                "SUPPORT_TICKET_READ_OWN",
                "AI_KNOWLEDGE_READ_CUSTOMER",
                "CUSTOMER_VEHICLE_READ_OWN",
                "WALLET_READ_OWN");
        var pending = new EffectiveAccountAccessPolicy.AccessState(
                "CUSTOM_SUPPORT", true, false, Set.of(), Set.of(), AccountStatus.PENDING, null,
                CustomerStatus.INACTIVE, CustomerApprovalStatus.PENDING);
        Set<String> expected = Set.of("SUPPORT_WIDGET_ACCESS_OWN", "SUPPORT_TICKET_READ_OWN");
        assertEquals(expected, policy.resolveEffectivePermissionCodes(pending, assigned));
        assertFalse(policy.canUseBusinessPermissions(pending));
        assertTrue(policy.canUseEffectivePermissions(pending));
    }

    @Test
    void rejectedCustomerReceivesOnlyExplicitSupportPermissions() {
        Set<String> assigned = Set.of(
                "SUPPORT_WIDGET_ACCESS_OWN",
                "SUPPORT_TICKET_READ_OWN",
                "AI_KNOWLEDGE_READ_CUSTOMER",
                "CUSTOMER_VEHICLE_READ_OWN",
                "WALLET_READ_OWN");
        var rejected = new EffectiveAccountAccessPolicy.AccessState(
                "ANOTHER_CUSTOM_ROLE", true, false, Set.of(), Set.of(), AccountStatus.PENDING, null,
                CustomerStatus.INACTIVE, CustomerApprovalStatus.REJECTED);

        assertEquals(Set.of("SUPPORT_WIDGET_ACCESS_OWN", "SUPPORT_TICKET_READ_OWN"),
                policy.resolveEffectivePermissionCodes(rejected, assigned));
        assertFalse(policy.canUseBusinessPermissions(rejected));
        assertTrue(policy.canUseEffectivePermissions(rejected));
    }

    @Test
    void pendingNonCustomerAccountCannotUseAnyAssignedPermission() {
        var pendingPartner = new EffectiveAccountAccessPolicy.AccessState(
                "PARTNER_ADMIN", true, true, Set.of(UUID.randomUUID()), Set.of(),
                AccountStatus.PENDING, null, null, null);

        assertFalse(policy.canUseEffectivePermissions(pendingPartner));
        assertTrue(policy.resolveEffectivePermissionCodes(
                pendingPartner,
                Set.of("SUPPORT_WIDGET_ACCESS_OWN", "ORGANIZATION_READ_ALL"))
                .isEmpty());
    }

    @Test
    void activeNonCustomerAccountRetainsMembershipBoundChatPermission() {
        var internalActor = state("CUSTOM_INTERNAL_ROLE", true, AccountStatus.ACTIVE);

        assertEquals(Set.of("CHAT_CONVERSATION_READ_OWN"),
                policy.resolveEffectivePermissionCodes(
                        internalActor, Set.of("CHAT_CONVERSATION_READ_OWN")));
    }

    @Test
    void inactiveApprovedCustomerLosesSupportAndBusinessPermissions() {
        var inactiveCustomer = new EffectiveAccountAccessPolicy.AccessState(
                "CUSTOM_ROLE", true, false, Set.of(), Set.of(), AccountStatus.ACTIVE, null,
                CustomerStatus.INACTIVE, CustomerApprovalStatus.APPROVED);

        assertTrue(policy.resolveEffectivePermissionCodes(
                inactiveCustomer,
                Set.of("CHAT_CONVERSATION_READ_OWN", "CUSTOMER_VEHICLE_READ_OWN"))
                .isEmpty());
    }

    @Test
    void approvedCustomRoleReceivesCustomerKnowledgeAndBusinessPermissions() {
        var approved = new EffectiveAccountAccessPolicy.AccessState(
                "CUSTOM_NON_DEFAULT_ROLE", true, false, Set.of(), Set.of(), AccountStatus.ACTIVE, null,
                CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED);
        assertEquals(Set.of("AI_KNOWLEDGE_READ_CUSTOMER", "CUSTOMER_VEHICLE_READ_OWN"),
                policy.resolveEffectivePermissionCodes(approved,
                        Set.of("AI_KNOWLEDGE_READ_CUSTOMER", "CUSTOMER_VEHICLE_READ_OWN")));
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
