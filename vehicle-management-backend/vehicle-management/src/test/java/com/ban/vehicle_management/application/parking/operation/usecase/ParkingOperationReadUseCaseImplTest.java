package com.ban.vehicle_management.application.parking.operation.usecase;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglevel.port.in.ParkingLevelPortIn;
import com.ban.vehicle_management.application.parking.zone.port.in.ZonePortIn;
import com.ban.vehicle_management.application.parking.lane.port.in.LanePortIn;
import com.ban.vehicle_management.application.parking.parkingspace.port.in.ParkingSpacePortIn;
import com.ban.vehicle_management.application.parking.parkingsummary.port.in.SpaceSummaryPortIn;
import com.ban.vehicle_management.application.parking.parkinglayout.port.in.ParkingLayoutPortIn;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.LevelMapResult;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceSummaryResult;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.ZoneLayoutResult;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.*;
import java.util.List;
import java.util.UUID;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingOperationReadUseCaseImplTest {
    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private ParkingLevelPortIn parkingLevelPortIn;
    @Mock private ZonePortIn zonePortIn;
    @Mock private LanePortIn lanePortIn;
    @Mock private ParkingSpacePortIn parkingSpacePortIn;
    @Mock private SpaceSummaryPortIn spaceSummaryPortIn;
    @Mock private ParkingLayoutPortIn parkingLayoutPortIn;
    @Mock private ParkingSpaceAllocationPortOut allocationPortOut;
    @InjectMocks private ParkingOperationReadUseCaseImpl useCase;
    private final UUID lotId = UUID.randomUUID();

    private void actor(String role, String permission) {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "operator", "operator@example.test", UUID.randomUUID(),
                role, true, true, Set.of(UUID.randomUUID()), Set.of(lotId), AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE, null, null, false, ApprovalRequestStatus.APPROVED,
                Set.of(permission, "PARKING_SCOPE_ASSIGNED")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPLOYEE", "PARKING_MANAGER", "PARTNER_ADMIN"})
    void readsActiveLanesUsingOperationalPermissionForAnyAssignedLot(String role) {
        actor(role, "PARKING_SESSION_CHECK_IN_ALL");
        Lane lane = new Lane();
        lane.setLaneId(UUID.randomUUID());
        lane.setStatus(LaneStatus.ACTIVE);
        when(lanePortIn.getLanes(null, lotId, LaneDirection.IN, LaneStatus.ACTIVE, null)).thenReturn(List.of(lane));
        assertEquals(List.of(lane), useCase.getLanes(lotId, LaneDirection.IN));
    }

    @Test
    void checkoutOnlyOperatorCanReadLevels() {
        actor("EMPLOYEE", "PARKING_SESSION_CHECK_OUT_ALL");
        assertEquals(List.of(), useCase.getLevels(lotId));
        verify(parkingLevelPortIn).getParkingLevels(lotId, null, null);
    }

    @Test
    void rejectsAllReadsBeforeDataAccessWhenPermissionIsMissing() {
        actor("EMPLOYEE", "PARKING_LOT_READ_ALL");
        assertThrows(AccessDeniedException.class, () -> useCase.getLevels(lotId));
        assertThrows(AccessDeniedException.class, () -> useCase.getZones(lotId, null));
        assertThrows(AccessDeniedException.class, () -> useCase.getLanes(lotId, LaneDirection.IN));
        assertThrows(AccessDeniedException.class, () -> useCase.getSpaces(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> useCase.getLevelMap(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> useCase.getLotSummary(lotId));
        assertThrows(AccessDeniedException.class, () -> useCase.getPublishedLayout(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> useCase.getAllocatedSpaceIds(UUID.randomUUID()));
        verifyNoInteractions(parkingLevelPortIn, zonePortIn, lanePortIn, parkingSpacePortIn,
                spaceSummaryPortIn, parkingLayoutPortIn, allocationPortOut);
    }

    @Test
    void preservesScopeDenialInsteadOfReturningAnEmptyLaneList() {
        actor("EMPLOYEE", "PARKING_SESSION_CHECK_IN_ALL");
        UUID otherLot = UUID.randomUUID();
        when(lanePortIn.getLanes(null, otherLot, LaneDirection.IN, LaneStatus.ACTIVE, null))
                .thenThrow(new AccessDeniedException("outside assigned lot"));
        assertThrows(AccessDeniedException.class, () -> useCase.getLanes(otherLot, LaneDirection.IN));
    }

    @Test
    void noAllocationReadIfZoneIsOutsideScope() {
        actor("EMPLOYEE", "PARKING_SESSION_CHECK_IN_ALL");
        UUID otherZone = UUID.randomUUID();
        when(parkingSpacePortIn.getParkingSpaces(otherZone, null, "ACTIVE"))
                .thenThrow(new AccessDeniedException("outside assigned lot"));
        assertThrows(AccessDeniedException.class, () -> useCase.getAllocatedSpaceIds(otherZone));
        verifyNoInteractions(allocationPortOut);
    }

    @Test
    void newEmptyZoneHasNoAllocatedSpaces() {
        actor("PARKING_MANAGER", "PARKING_SPACE_READ_ALL");
        assertEquals(List.of(), useCase.getAllocatedSpaceIds(UUID.randomUUID()));
        verifyNoInteractions(allocationPortOut);
    }
}
