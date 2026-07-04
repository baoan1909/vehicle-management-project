package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProvisionedAccountPolicyTest {

    private final ProvisionedAccountPolicy policy = new ProvisionedAccountPolicy();

    @Test
    void shouldResolveManagedTargetRolesByCurrentRole() {
        assertEquals(
                Set.of(
                        AdminProvisionableAccountRoleCode.SYSTEM_ADMIN,
                        AdminProvisionableAccountRoleCode.PARKING_MANAGER
                ),
                policy.managedTargetRoles(AdminProvisionableAccountRoleCode.SYSTEM_ADMIN)
        );
        assertEquals(
                Set.of(
                        AdminProvisionableAccountRoleCode.EMPLOYEE,
                        AdminProvisionableAccountRoleCode.CUSTOMER
                ),
                policy.managedTargetRoles(AdminProvisionableAccountRoleCode.PARKING_MANAGER)
        );
        assertTrue(policy.managedTargetRoles(AdminProvisionableAccountRoleCode.EMPLOYEE).isEmpty());
    }

    @Test
    void shouldMapAccountStatusToRelatedStatuses() {
        assertEquals(UserProfileStatus.SUSPENDED, policy.toUserProfileStatus(AccountStatus.LOCKED));
        assertEquals(
                EmployeeStatus.SUSPENDED,
                policy.toEmployeeStatus(AdminProvisionableAccountRoleCode.EMPLOYEE, AccountStatus.LOCKED)
        );
        assertNull(policy.toEmployeeStatus(AdminProvisionableAccountRoleCode.CUSTOMER, AccountStatus.LOCKED));
    }

    @Test
    void shouldRejectUnsupportedPendingProvisionedStatus() {
        assertThrows(BadRequestException.class, () -> policy.toUserProfileStatus(AccountStatus.PENDING));
    }

    @Test
    void shouldRejectRoleTransitionBetweenInternalAndCustomer() {
        assertThrows(
                BadRequestException.class,
                () -> policy.validateRoleTransition(
                        AdminProvisionableAccountRoleCode.EMPLOYEE,
                        AdminProvisionableAccountRoleCode.CUSTOMER
                )
        );
    }

    @Test
    void shouldRejectDisabledToLockedStatusTransition() {
        assertThrows(
                BadRequestException.class,
                () -> policy.validateStatusTransition(AccountStatus.DISABLED, AccountStatus.LOCKED)
        );
    }
}
