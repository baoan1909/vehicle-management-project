package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccessState;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.model.RequiredAccessAction;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CurrentAccessStatePolicyTest {
    private final CurrentAccessStatePolicy policy = new CurrentAccessStatePolicy();

    @Test
    void precedenceIsAccountThenRoleThenApprovalThenOnboardingThenLifecycleThenPermissions() {
        assertState(access(AccountStatus.DISABLED, false, null, null, null, true, Set.of()),
                CurrentAccessState.ACCOUNT_DISABLED, RequiredAccessAction.CONTACT_ADMIN);
        assertState(access(AccountStatus.LOCKED, false, null, null, null, true, Set.of()),
                CurrentAccessState.ACCOUNT_LOCKED, RequiredAccessAction.CONTACT_ADMIN);
        assertState(access(AccountStatus.PENDING, false, null, null, null, true, Set.of()),
                CurrentAccessState.ACCOUNT_PENDING, RequiredAccessAction.WAIT_FOR_APPROVAL);
        assertState(access(AccountStatus.ACTIVE, false, null, null, null, true, Set.of()),
                CurrentAccessState.ROLE_DISABLED, RequiredAccessAction.CONTACT_ADMIN);
        assertState(access(AccountStatus.ACTIVE, true, null, CustomerStatus.ACTIVE,
                        CustomerApprovalStatus.PENDING, true, Set.of("CUSTOMER_VEHICLE_READ_OWN")),
                CurrentAccessState.CUSTOMER_APPROVAL_REQUIRED, RequiredAccessAction.WAIT_FOR_APPROVAL);
        assertState(access(AccountStatus.ACTIVE, true, null, null, null, true, Set.of("ACCOUNT_READ_ALL")),
                CurrentAccessState.ONBOARDING_REQUIRED, RequiredAccessAction.COMPLETE_PROFILE);
        assertState(access(AccountStatus.ACTIVE, true, EmployeeStatus.INACTIVE, null, null, false,
                        Set.of("ACCOUNT_READ_ALL")),
                CurrentAccessState.EMPLOYEE_INACTIVE, RequiredAccessAction.CONTACT_ADMIN);
        assertState(access(AccountStatus.ACTIVE, true, null, null, null, false, Set.of()),
                CurrentAccessState.NO_EFFECTIVE_PERMISSIONS, RequiredAccessAction.CONTACT_ADMIN);
        assertState(access(AccountStatus.ACTIVE, true, null, null, null, false,
                        Set.of("ACCOUNT_READ_ALL")),
                CurrentAccessState.ACTIVE, null);
    }

    @Test
    void missingOrSuspendedOrganizationIsReportedFromMissingActiveScope() {
        assertState(access(AccountStatus.ACTIVE, true, null, null, null, false,
                        Set.of("PARKING_SCOPE_PARTNER", "PARKING_LOT_READ_ALL")),
                CurrentAccessState.ORGANIZATION_ACCESS_REQUIRED,
                RequiredAccessAction.CONTACT_ORGANIZATION_ADMIN);
    }

    private void assertState(
            CurrentAccountAccess access, CurrentAccessState state, RequiredAccessAction action) {
        CurrentAccessStatePolicy.Decision decision = policy.resolve(access, access.onboardingRequired());
        assertEquals(state, decision.state());
        assertEquals(action, decision.requiredAction());
    }

    private CurrentAccountAccess access(
            AccountStatus accountStatus, boolean roleActive, EmployeeStatus employeeStatus,
            CustomerStatus customerStatus, CustomerApprovalStatus approvalStatus,
            boolean onboarding, Set<String> permissions) {
        return new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "user", "user@example.com", UUID.randomUUID(),
                "ANY_ROLE", roleActive, false, Set.of(), Set.of(), accountStatus, employeeStatus,
                customerStatus, approvalStatus, onboarding, permissions);
    }
}
