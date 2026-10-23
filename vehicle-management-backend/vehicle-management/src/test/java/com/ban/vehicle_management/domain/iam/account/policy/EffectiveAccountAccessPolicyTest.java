package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EffectiveAccountAccessPolicyTest {

    private final EffectiveAccountAccessPolicy policy = new EffectiveAccountAccessPolicy();

    @ParameterizedTest
    @MethodSource("lifecycleStates")
    void shouldApplyRoleSpecificLifecycleRules(
            String roleCode,
            boolean roleActive,
            AccountStatus accountStatus,
            EmployeeStatus employeeStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus customerApprovalStatus,
            boolean expected
    ) {
        EffectiveAccountAccessPolicy.AccessState state = new EffectiveAccountAccessPolicy.AccessState(
                roleCode,
                roleActive,
                accountStatus,
                employeeStatus,
                customerStatus,
                customerApprovalStatus
        );

        assertEquals(expected, policy.canUseBusinessPermissions(state));
    }

    @Test
    void shouldReturnDeterministicEffectivePermissionsOnlyWhenEligible() {
        EffectiveAccountAccessPolicy.AccessState eligible = new EffectiveAccountAccessPolicy.AccessState(
                "SYSTEM_ADMIN",
                true,
                AccountStatus.ACTIVE,
                null,
                null,
                null
        );
        EffectiveAccountAccessPolicy.AccessState denied = new EffectiveAccountAccessPolicy.AccessState(
                "SYSTEM_ADMIN",
                true,
                AccountStatus.LOCKED,
                null,
                null,
                null
        );

        Set<String> effective = policy.resolveEffectivePermissionCodes(
                eligible,
                List.of("ROLE_UPDATE_ALL", "ACCOUNT_READ_ALL", "ROLE_UPDATE_ALL")
        );

        assertEquals(List.of("ACCOUNT_READ_ALL", "ROLE_UPDATE_ALL"), List.copyOf(effective));
        assertTrue(policy.resolveEffectivePermissionCodes(denied, effective).isEmpty());
    }

    @Test
    void shouldDenyNullState() {
        assertFalse(policy.canUseBusinessPermissions(null));
    }

    private static Stream<Arguments> lifecycleStates() {
        return Stream.of(
                Arguments.of("SYSTEM_ADMIN", true, AccountStatus.ACTIVE, null, null, null, true),
                Arguments.of("PARTNER_ADMIN", true, AccountStatus.ACTIVE, null, null, null, true),
                Arguments.of("SYSTEM_ADMIN", false, AccountStatus.ACTIVE, null, null, null, false),
                Arguments.of("SYSTEM_ADMIN", true, AccountStatus.PENDING, null, null, null, false),
                Arguments.of("PARKING_MANAGER", true, AccountStatus.ACTIVE, EmployeeStatus.ACTIVE, null, null, true),
                Arguments.of("EMPLOYEE", true, AccountStatus.ACTIVE, EmployeeStatus.ACTIVE, null, null, true),
                Arguments.of("EMPLOYEE", true, AccountStatus.ACTIVE, EmployeeStatus.INACTIVE, null, null, false),
                Arguments.of("EMPLOYEE", true, AccountStatus.ACTIVE, EmployeeStatus.SUSPENDED, null, null, false),
                Arguments.of("EMPLOYEE", true, AccountStatus.ACTIVE, null, null, null, false),
                Arguments.of("CUSTOMER", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, true),
                Arguments.of("CUSTOMER", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.INACTIVE, CustomerApprovalStatus.APPROVED, false),
                Arguments.of("CUSTOMER", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING, false),
                Arguments.of("CUSTOMER", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.REJECTED, false),
                Arguments.of("CUSTOMER", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.SUSPENDED, false),
                Arguments.of("CUSTOMER", true, AccountStatus.ACTIVE, null, null, null, false),
                Arguments.of(null, true, AccountStatus.ACTIVE, null, null, null, false),
                Arguments.of(" ", true, AccountStatus.ACTIVE, null, null, null, false),
                Arguments.of("customer", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, false),
                Arguments.of(" CUSTOMER ", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, false),
                Arguments.of("CUSTOM_APPROVER", true, AccountStatus.ACTIVE, null, null, null, true),
                Arguments.of("CUSTOM_OPERATIONS", true, AccountStatus.ACTIVE,
                        EmployeeStatus.ACTIVE, null, null, true),
                Arguments.of("CUSTOM_OPERATIONS", true, AccountStatus.ACTIVE,
                        EmployeeStatus.INACTIVE, null, null, false),
                Arguments.of("CUSTOM_CUSTOMER", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, true),
                Arguments.of("CUSTOM_CUSTOMER", true, AccountStatus.ACTIVE, null,
                        CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING, false),
                Arguments.of("GUEST", true, AccountStatus.ACTIVE, null, null, null, true)
        );
    }
}
