package com.ban.vehicle_management.application.iam.organization.authorization;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessGuardTest {
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock OrganizationPortOut organizationPortOut;
    @InjectMocks OrganizationAccessGuard guard;

    @Test
    void customAndStandardRolesWithSameAssignedScopeBehaveTheSame() {
        UUID lotId = UUID.randomUUID();
        ParkingLot lot = parkingLot(lotId, UUID.randomUUID());
        CurrentAccountAccess standard = access("PARKING_MANAGER",
                Set.of("PARKING_SCOPE_ASSIGNED"), Set.of(), Set.of(lotId), EmployeeStatus.ACTIVE);
        CurrentAccountAccess custom = access("CUSTOM_OPERATOR",
                Set.of("PARKING_SCOPE_ASSIGNED"), Set.of(), Set.of(lotId), EmployeeStatus.ACTIVE);

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(standard, custom);
        assertDoesNotThrow(() -> guard.ensureCanOperateParkingLot(lot));
        assertDoesNotThrow(() -> guard.ensureCanOperateParkingLot(lot));
    }

    @Test
    void assignedScopeCannotAccessAnotherParkingLot() {
        UUID assigned = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                "CUSTOM_OPERATOR", Set.of("PARKING_SCOPE_ASSIGNED"), Set.of(), Set.of(assigned),
                EmployeeStatus.ACTIVE));
        assertThrows(AccessDeniedException.class,
                () -> guard.ensureCanAccessParkingLot(parkingLot(UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void partnerScopeRequiresMatchingOrganization() {
        UUID ownOrganization = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                "CUSTOM_PARTNER", Set.of("PARKING_SCOPE_PARTNER"), Set.of(ownOrganization),
                Set.of(), null));
        assertDoesNotThrow(() -> guard.ensureCanOperateParkingLot(
                parkingLot(UUID.randomUUID(), ownOrganization)));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperateParkingLot(
                parkingLot(UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void roleNamedPartnerAdminWithoutActionPermissionCannotCreateLot() {
        UUID organizationId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                "PARTNER_ADMIN", Set.of("PARKING_SCOPE_PARTNER"), Set.of(organizationId), Set.of(), null));
        assertThrows(AccessDeniedException.class,
                () -> guard.resolveOrganizationIdForParkingLotCreation(organizationId));
    }

    @Test
    void platformScopeDoesNotImplicitlyAllowOperationalWrite() {
        ParkingLot lot = parkingLot(UUID.randomUUID(), UUID.randomUUID());
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                "SYSTEM_ADMIN", Set.of("PARKING_SCOPE_PLATFORM"), Set.of(), Set.of(), null));
        assertDoesNotThrow(() -> guard.ensureCanAccessParkingLot(lot));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperateParkingLot(lot));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanConfigureParkingLot(lot));
    }

    @Test
    void createLotRequiresActionPartnerScopeAndActiveResourceOrganization() {
        UUID organizationId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setOrganizationId(organizationId);
        organization.setStatus(OrganizationStatus.ACTIVE);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                "CUSTOM_PARTNER", Set.of("PARKING_LOT_CREATE_ALL", "PARKING_SCOPE_PARTNER"),
                Set.of(organizationId), Set.of(), null));
        when(organizationPortOut.findById(organizationId)).thenReturn(java.util.Optional.of(organization));
        assertEquals(organizationId, guard.resolveOrganizationIdForParkingLotCreation(organizationId));
    }

    private CurrentAccountAccess access(
            String roleCode, Set<String> permissions, Set<UUID> organizations,
            Set<UUID> lots, EmployeeStatus employeeStatus) {
        return new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "user", "user@example.com", UUID.randomUUID(),
                roleCode, true, !organizations.isEmpty(), organizations, lots, AccountStatus.ACTIVE,
                employeeStatus, null, null, false, permissions);
    }

    private ParkingLot parkingLot(UUID lotId, UUID organizationId) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(organizationId);
        return lot;
    }
}
