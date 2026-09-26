package com.ban.vehicle_management.application.people.employee.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class EmployeeOrganizationAccessGuardTest {

    @Mock private EmployeePortOut employeePortOut;
    @Mock private OrganizationPortOut organizationPortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @InjectMocks private EmployeeOrganizationAccessGuard guard;

    @Test
    void employeeCanBeScheduledOnlyWithinOwningPartner() {
        UUID employeeId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(organizationId);
        when(employeePortOut.findAccountIdByEmployeeId(employeeId)).thenReturn(Optional.of(accountId));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(accountId))
                .thenReturn(Set.of(organizationId), Set.of(UUID.randomUUID()));

        assertDoesNotThrow(() -> guard.ensureBelongsToParkingLot(employeeId, lotId));
        assertThrows(AccessDeniedException.class,
                () -> guard.ensureBelongsToParkingLot(employeeId, lotId));
    }

    @Test
    void unknownEmployeeOwnershipIsDenied() {
        UUID employeeId = UUID.randomUUID();
        when(employeePortOut.findAccountIdByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> guard.ensureBelongsToParkingLot(employeeId, UUID.randomUUID()));
    }
}
