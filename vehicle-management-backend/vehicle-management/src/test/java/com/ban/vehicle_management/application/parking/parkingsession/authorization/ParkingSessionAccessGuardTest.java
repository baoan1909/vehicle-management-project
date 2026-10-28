package com.ban.vehicle_management.application.parking.parkingsession.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
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
    void roleNamedSystemAdminIsAllowedWhenExplicitlyGrantedCheckInPermission() {
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
    void actionCheckUsesPermissionRatherThanRoleLifecycle() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                AdminProvisionableAccountRoleCode.EMPLOYEE.name(),
                EmployeeStatus.INACTIVE,
                Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
        ));

        assertDoesNotThrow(accessGuard::ensureCanCheckIn);
    }

    @Test
    void partnerAdminMustPassParkingLotOrganizationScope() {
        OrganizationAccessGuard organizationGuard = mock(OrganizationAccessGuard.class);
        ParkingLot lot = new ParkingLot();
        ParkingSessionAccessGuard accessGuard = new ParkingSessionAccessGuard(
                new FakeCurrentAccountPortIn(currentAccount(
                        "PARTNER_ADMIN", null, Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
                )),
                organizationGuard,
                mock(EmployeeParkingLotAccessGuard.class)
        );

        accessGuard.ensureCanOperateParkingLot(lot);
        verify(organizationGuard).ensureCanOperateParkingLot(lot);

        doThrow(new AccessDeniedException("Other partner's parking lot"))
                .when(organizationGuard).ensureCanOperateParkingLot(lot);
        assertThrows(AccessDeniedException.class, () -> accessGuard.ensureCanOperateParkingLot(lot));
    }

    @Test
    void partnerAdminCannotUseCardFromAnotherLot() {
        ParkingSessionAccessGuard accessGuard = accessGuard(currentAccount(
                "PARTNER_ADMIN", null, Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL)
        ));
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(UUID.randomUUID());
        Card card = new Card();
        card.setParkingLotId(UUID.randomUUID());

        assertThrows(AccessDeniedException.class, () -> accessGuard.ensureCanUseCardInParkingLot(card, lot));
        card.setParkingLotId(lot.getParkingLotId());
        assertDoesNotThrow(() -> accessGuard.ensureCanUseCardInParkingLot(card, lot));
    }

    @Test
    void employeeOperatingParkingLotDelegatesToActiveShiftScopeCheck() {
        OrganizationAccessGuard organizationGuard = mock(OrganizationAccessGuard.class);
        EmployeeParkingLotAccessGuard employeeGuard = mock(EmployeeParkingLotAccessGuard.class);
        ParkingSessionAccessGuard accessGuard = new ParkingSessionAccessGuard(
                new FakeCurrentAccountPortIn(currentAccount("EMPLOYEE", EmployeeStatus.ACTIVE,
                        Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL))),
                organizationGuard, employeeGuard);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(UUID.randomUUID());

        accessGuard.ensureCanOperateParkingLot(lot);
        verify(employeeGuard).ensureCanOperate(lot.getParkingLotId());
        verifyNoInteractions(organizationGuard);
        doThrow(new AccessDeniedException("other lot"))
                .when(employeeGuard).ensureCanOperate(lot.getParkingLotId());
        assertThrows(AccessDeniedException.class, () -> accessGuard.ensureCanOperateParkingLot(lot));
    }

    @Test
    void nonEmployeeOperatingParkingLotDelegatesToOrganizationScopeCheck() {
        OrganizationAccessGuard organizationGuard = mock(OrganizationAccessGuard.class);
        EmployeeParkingLotAccessGuard employeeGuard = mock(EmployeeParkingLotAccessGuard.class);
        ParkingSessionAccessGuard accessGuard = new ParkingSessionAccessGuard(
                new FakeCurrentAccountPortIn(currentAccount("PARKING_LOT_MANAGER", EmployeeStatus.ACTIVE,
                        Set.of(ParkingSessionAccessGuard.PARKING_SESSION_CHECK_IN_ALL))),
                organizationGuard, employeeGuard);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(UUID.randomUUID());

        accessGuard.ensureCanOperateParkingLot(lot);

        verify(organizationGuard).ensureCanOperateParkingLot(lot);
        verifyNoInteractions(employeeGuard);
    }

    private ParkingSessionAccessGuard accessGuard(CurrentAccountAccess currentAccount) {
        return new ParkingSessionAccessGuard(
                new FakeCurrentAccountPortIn(currentAccount),
                mock(OrganizationAccessGuard.class),
                mock(EmployeeParkingLotAccessGuard.class)
        );
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
                true,
                "PARTNER_ADMIN".equals(roleCode),
                AccountStatus.ACTIVE,
                employeeStatus,
                null,
                null,
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
