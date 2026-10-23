package com.ban.vehicle_management.domain.iam.account.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class CurrentAccountAccessTest {

    private static final Set<String> ASSIGNED_PERMISSIONS = Set.of("ACCOUNT_READ_ALL");

    @ParameterizedTest
    @MethodSource("accountOnlyRoleLifecycle")
    void accountOnlyRolesShouldRequireActiveAccount(
            String roleCode,
            AccountStatus accountStatus,
            boolean expected
    ) {
        CurrentAccountAccess access = access(
                roleCode,
                accountStatus,
                null,
                null,
                null
        );

        assertEquals(expected, access.canUseBusinessPermissions());
        assertEquals(expected ? ASSIGNED_PERMISSIONS : Set.of(), access.getEffectivePermissionCodes());
    }

    @ParameterizedTest
    @MethodSource("employeeRoleLifecycle")
    void employeeBackedRolesShouldRequireActiveAccountAndEmployee(
            String roleCode,
            AccountStatus accountStatus,
            EmployeeStatus employeeStatus,
            boolean expected
    ) {
        CurrentAccountAccess access = access(
                roleCode,
                accountStatus,
                employeeStatus,
                null,
                null
        );

        assertEquals(expected, access.canUseBusinessPermissions());
        assertEquals(expected ? ASSIGNED_PERMISSIONS : Set.of(), access.getEffectivePermissionCodes());
    }

    @ParameterizedTest
    @MethodSource("customerLifecycle")
    void customerShouldRequireActiveApprovedCustomerRecord(
            AccountStatus accountStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus approvalStatus,
            boolean expected
    ) {
        CurrentAccountAccess access = access(
                "CUSTOMER",
                accountStatus,
                null,
                customerStatus,
                approvalStatus
        );

        assertEquals(expected, access.canUseBusinessPermissions());
        assertEquals(expected ? ASSIGNED_PERMISSIONS : Set.of(), access.getEffectivePermissionCodes());
    }

    @ParameterizedTest
    @MethodSource("invalidBusinessRoles")
    void invalidBusinessRoleShouldFailClosed(String roleCode) {
        CurrentAccountAccess access = access(
                roleCode,
                AccountStatus.ACTIVE,
                null,
                null,
                null
        );

        assertFalse(access.canUseBusinessPermissions());
        assertTrue(access.getEffectivePermissionCodes().isEmpty());
    }

    @Test
    void inactiveRoleShouldFailClosed() {
        CurrentAccountAccess access = new CurrentAccountAccess(
                UUID.randomUUID(),
                "subject",
                "system.admin",
                "system.admin@example.com",
                UUID.randomUUID(),
                "SYSTEM_ADMIN",
                false,
                AccountStatus.ACTIVE,
                null,
                null,
                null,
                ASSIGNED_PERMISSIONS
        );

        assertFalse(access.canUseBusinessPermissions());
        assertTrue(access.getEffectivePermissionCodes().isEmpty());
    }

    private static Stream<Arguments> accountOnlyRoleLifecycle() {
        return Stream.of("SYSTEM_ADMIN", "PARTNER_ADMIN")
                .flatMap(roleCode -> Stream.of(
                        Arguments.of(roleCode, AccountStatus.ACTIVE, true),
                        Arguments.of(roleCode, AccountStatus.PENDING, false),
                        Arguments.of(roleCode, AccountStatus.LOCKED, false),
                        Arguments.of(roleCode, AccountStatus.DISABLED, false)
                ));
    }

    private static Stream<Arguments> employeeRoleLifecycle() {
        return Stream.of("PARKING_MANAGER", "EMPLOYEE")
                .flatMap(roleCode -> Stream.of(
                        Arguments.of(roleCode, AccountStatus.ACTIVE, EmployeeStatus.ACTIVE, true),
                        Arguments.of(roleCode, AccountStatus.ACTIVE, EmployeeStatus.INACTIVE, false),
                        Arguments.of(roleCode, AccountStatus.ACTIVE, EmployeeStatus.SUSPENDED, false),
                        Arguments.of(roleCode, AccountStatus.ACTIVE, null, false),
                        Arguments.of(roleCode, AccountStatus.PENDING, EmployeeStatus.ACTIVE, false),
                        Arguments.of(roleCode, AccountStatus.LOCKED, EmployeeStatus.ACTIVE, false),
                        Arguments.of(roleCode, AccountStatus.DISABLED, EmployeeStatus.ACTIVE, false)
                ));
    }

    private static Stream<Arguments> customerLifecycle() {
        return Stream.of(
                Arguments.of(AccountStatus.ACTIVE, CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, true),
                Arguments.of(AccountStatus.ACTIVE, CustomerStatus.INACTIVE, CustomerApprovalStatus.APPROVED, false),
                Arguments.of(AccountStatus.ACTIVE, CustomerStatus.ACTIVE, CustomerApprovalStatus.PENDING, false),
                Arguments.of(AccountStatus.ACTIVE, CustomerStatus.ACTIVE, CustomerApprovalStatus.REJECTED, false),
                Arguments.of(AccountStatus.ACTIVE, CustomerStatus.ACTIVE, CustomerApprovalStatus.SUSPENDED, false),
                Arguments.of(AccountStatus.ACTIVE, null, null, false),
                Arguments.of(AccountStatus.PENDING, CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, false),
                Arguments.of(AccountStatus.LOCKED, CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, false),
                Arguments.of(AccountStatus.DISABLED, CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED, false)
        );
    }

    private static Stream<String> invalidBusinessRoles() {
        return Stream.of(null, "", "   ", "custom_role", " CUSTOM_ROLE ");
    }

    @Test
    void activeCustomRoleShouldKeepAssignedPermissions() {
        CurrentAccountAccess access = access(
                "CUSTOM_OPERATOR",
                AccountStatus.ACTIVE,
                null,
                null,
                null
        );

        assertTrue(access.canUseBusinessPermissions());
        assertEquals(ASSIGNED_PERMISSIONS, access.getEffectivePermissionCodes());
    }

    private CurrentAccountAccess access(
            String roleCode,
            AccountStatus accountStatus,
            EmployeeStatus employeeStatus,
            CustomerStatus customerStatus,
            CustomerApprovalStatus approvalStatus
    ) {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "subject",
                "user",
                "user@example.com",
                UUID.randomUUID(),
                roleCode,
                accountStatus,
                employeeStatus,
                customerStatus,
                approvalStatus,
                ASSIGNED_PERMISSIONS
        );
    }
}
