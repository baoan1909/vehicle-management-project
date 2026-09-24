package com.ban.vehicle_management.application.iam.organization.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
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

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private OrganizationPortOut organizationPortOut;

    @InjectMocks
    private OrganizationAccessGuard organizationAccessGuard;

    @Test
    void shouldAllowParkingManagerToAccessOnlyAssignedParkingLot() {
        UUID accountId = UUID.randomUUID();
        UUID assignedParkingLotId = UUID.randomUUID();
        UUID otherParkingLotId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount(
                accountId,
                OrganizationAccessGuard.PARKING_MANAGER
        ));
        when(organizationPortOut.findScopedParkingLotIdsByAccountId(accountId)).thenReturn(Set.of(assignedParkingLotId));

        assertDoesNotThrow(() -> organizationAccessGuard.ensureCanAccessParkingLot(parkingLot(assignedParkingLotId)));
        assertThrows(
                AccessDeniedException.class,
                () -> organizationAccessGuard.ensureCanAccessParkingLot(parkingLot(otherParkingLotId))
        );
    }

    @Test
    void shouldRejectPartnerAdminCreatingParkingLotForAnotherOrganization() {
        UUID accountId = UUID.randomUUID();
        UUID ownOrganizationId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount(
                accountId,
                OrganizationAccessGuard.PARTNER_ADMIN
        ));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(accountId)).thenReturn(Set.of(ownOrganizationId));

        assertThrows(
                AccessDeniedException.class,
                () -> organizationAccessGuard.resolveOrganizationIdForParkingLotCreation(otherOrganizationId)
        );
    }

    private ParkingLot parkingLot(UUID parkingLotId) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(parkingLotId);
        parkingLot.setOrganizationId(UUID.randomUUID());
        return parkingLot;
    }

    private CurrentAccountAccess currentAccount(UUID accountId, String roleCode) {
        return new CurrentAccountAccess(
                accountId,
                "subject",
                "user",
                "user@example.com",
                UUID.randomUUID(),
                roleCode,
                AccountStatus.ACTIVE,
                OrganizationAccessGuard.PARKING_MANAGER.equals(roleCode) ? EmployeeStatus.ACTIVE : null,
                Set.of()
        );
    }
}
