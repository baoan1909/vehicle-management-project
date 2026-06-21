package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountOnboardingPolicyTest {

    private final AccountOnboardingPolicy policy = new AccountOnboardingPolicy();

    @Test
    void shouldRequireEmployeeRecordForInternalEmployeeOnboarding() {
        AccountProfileState state = state(AdminProvisionableAccountRoleCode.EMPLOYEE, UUID.randomUUID(), null, null);

        assertTrue(policy.isOnboardingRequired(state, false));
    }

    @Test
    void shouldRequireCustomerRecordForCustomerOnboarding() {
        AccountProfileState state = state(AdminProvisionableAccountRoleCode.CUSTOMER, UUID.randomUUID(), null, null);

        assertTrue(policy.isOnboardingRequired(state, false));
    }

    @Test
    void shouldRequirePendingSystemAdminApprovalWhenRequestDoesNotExist() {
        AccountProfileState state = state(
                AdminProvisionableAccountRoleCode.SYSTEM_ADMIN,
                UUID.randomUUID(),
                null,
                null
        );

        assertTrue(policy.needsSystemAdminApprovalLookup(state));
        assertTrue(policy.isOnboardingRequired(state, false));
        assertFalse(policy.isOnboardingRequired(state, true));
    }

    @Test
    void shouldResolveDefaultJobTitleByRole() {
        assertEquals("Parking Staff", policy.defaultJobTitle(AdminProvisionableAccountRoleCode.EMPLOYEE));
        assertEquals("Parking Manager", policy.defaultJobTitle(AdminProvisionableAccountRoleCode.PARKING_MANAGER));
    }

    private AccountProfileState state(
            AdminProvisionableAccountRoleCode roleCode,
            UUID userProfileId,
            UUID employeeId,
            UUID customerId
    ) {
        return new AccountProfileState(
                UUID.randomUUID(),
                "user",
                "user@example.com",
                "keycloak-user",
                roleCode.name(),
                userProfileId,
                userProfileId == null ? null : "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                userProfileId == null ? null : UserProfileStatus.ACTIVE,
                employeeId,
                null,
                null,
                null,
                null,
                customerId,
                null,
                null,
                null,
                null,
                AccountStatus.PENDING
        );
    }
}
