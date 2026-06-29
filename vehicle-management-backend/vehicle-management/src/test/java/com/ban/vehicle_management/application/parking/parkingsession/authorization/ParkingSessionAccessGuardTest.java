package com.ban.vehicle_management.application.parking.parkingsession.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ParkingSessionAccessGuardTest {

    private static final String PARKING_SESSION_CREATE_ALL = "PARKING_SESSION_CREATE_ALL";
    private static final String PARKING_EVENT_CREATE_ALL = "PARKING_EVENT_CREATE_ALL";

    @Test
    void shouldAllowParkingManagerWhenExplicitlyGrantedCheckInPermission() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.PARKING_MANAGER.name(),
                EmployeeStatus.ACTIVE,
                Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
        ));

        assertDoesNotThrow(accessGuard::ensureCanCheckIn);
    }

    @Test
    void shouldAllowCheckInForActiveEmployeeWithCheckInPermission() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.EMPLOYEE.name(),
                EmployeeStatus.ACTIVE,
                Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
        ));

        assertDoesNotThrow(accessGuard::ensureCanCheckIn);
    }

    @Test
    void shouldAllowCustomRoleWhenExplicitlyGrantedCheckInPermission() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                "GATE_OPERATOR",
                null,
                Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
        ));

        assertDoesNotThrow(accessGuard::ensureCanCheckIn);
    }

    @Test
    void shouldAllowSystemAdminWhenExplicitlyGrantedCheckInPermission() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name(),
                null,
                Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
        ));

        assertDoesNotThrow(accessGuard::ensureCanCheckIn);
    }

    @Test
    void shouldAllowEmployeeWhenExplicitlyGrantedCheckOutPermission() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.EMPLOYEE.name(),
                EmployeeStatus.ACTIVE,
                Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_OUT_ALL)
        ));

        assertDoesNotThrow(accessGuard::ensureCanCheckOut);
    }

    @Test
    void shouldRejectEmployeeWithoutCheckOutPermission() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.EMPLOYEE.name(),
                EmployeeStatus.ACTIVE,
                Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
        ));

        assertThrows(AccessDeniedException.class, accessGuard::ensureCanCheckOut);
    }

    @Test
    void shouldRejectSystemAdminWithOnlyCrudParkingPermissions() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name(),
                null,
                legacyCrudParkingPermissions()
        ));

        assertThrows(AccessDeniedException.class, accessGuard::ensureCanCheckIn);
    }

    @Test
    void shouldRejectCustomRoleWithOnlyCrudParkingPermissions() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                "SECURITY_GUARD",
                null,
                legacyCrudParkingPermissions()
        ));

        assertThrows(AccessDeniedException.class, accessGuard::ensureCanCheckIn);
    }

    @Test
    void shouldRejectCustomerWithoutCheckInPermission() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.CUSTOMER.name(),
                null,
                Set.of()
        ));

        assertThrows(AccessDeniedException.class, accessGuard::ensureCanCheckIn);
    }

    @Test
    void shouldRejectInactiveEmployeeWithCheckInPermission() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.EMPLOYEE.name(),
                EmployeeStatus.INACTIVE,
                Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
        ));

        assertThrows(AccessDeniedException.class, accessGuard::ensureCanCheckIn);
    }

    private ParkingSessionAccessGuard accessGuard(CurrentAccountAccess currentAccount) {
        return new ParkingSessionAccessGuard(new FakeCurrentAccountPortIn(currentAccount));
    }

    private CurrentAccountAccess currentAccount(
            String roleCode,
            EmployeeStatus employeeStatus,
            Set<String> permissionCodes
    ) {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "subject",
                roleCode.toLowerCase(),
                roleCode.toLowerCase() + "@example.com",
                UUID.randomUUID(),
                roleCode,
                AccountStatus.ACTIVE,
                employeeStatus,
                permissionCodes
        );
    }

    private Set<String> legacyCrudParkingPermissions() {
        return Set.of(PARKING_SESSION_CREATE_ALL, PARKING_EVENT_CREATE_ALL);
    }

    private static class FakeCurrentAccountPortIn implements CurrentAccountPortIn {

        private final CurrentAccountAccess currentAccount;

        private FakeCurrentAccountPortIn(CurrentAccountAccess currentAccount) {
            this.currentAccount = currentAccount;
        }

        @Override
        public Optional<CurrentAccountAccess> getCurrentAccount() {
            return Optional.ofNullable(currentAccount);
        }

        @Override
        public CurrentAccountAccess getCurrentAccountOrThrow() {
            return getCurrentAccount().orElseThrow(() -> new AccessDeniedException("Access is denied"));
        }

        @Override
        public Optional<UUID> getCurrentAccountId() {
            return getCurrentAccount().map(CurrentAccountAccess::accountId);
        }

        @Override
        public UUID getCurrentAccountIdOrThrow() {
            return getCurrentAccountOrThrow().accountId();
        }

        @Override
        public boolean hasPermission(String permissionCode) {
            return getCurrentAccount()
                    .filter(CurrentAccountAccess::canUseBusinessPermissions)
                    .map(CurrentAccountAccess::permissionCodes)
                    .orElseGet(Set::of)
                    .contains(permissionCode);
        }

        @Override
        public void requirePermission(String permissionCode) {
            if (!hasPermission(permissionCode)) {
                throw new AccessDeniedException("Access is denied");
            }
        }
    }
}
