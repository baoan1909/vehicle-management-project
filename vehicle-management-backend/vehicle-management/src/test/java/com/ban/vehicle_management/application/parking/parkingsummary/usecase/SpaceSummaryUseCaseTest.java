package com.ban.vehicle_management.application.parking.parkingsummary.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceSummaryResult;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SpaceSummaryUseCaseTest {

    @Mock private ZonePortOut zonePortOut;
    @Mock private ParkingLevelPortOut parkingLevelPortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ParkingSpacePortOut parkingSpacePortOut;
    @Mock private ParkingSpaceAllocationPortOut allocationPortOut;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;
    @Mock private OrganizationAccessGuard organizationAccessGuard;

    @InjectMocks private SpaceSummaryUseCaseImpl useCase;

    @Test
    void shouldSummarizeSpaceZoneWithOccupancySplit() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingSpace available = space(zoneId, "A-01", ParkingSpaceStatus.AVAILABLE, null);
        ParkingSpace visitor = space(zoneId, "A-02", ParkingSpaceStatus.OCCUPIED, ManualOccupancyType.VISITOR);
        ParkingSpace registered = space(zoneId, "A-03", ParkingSpaceStatus.OCCUPIED, ManualOccupancyType.REGISTERED);
        ParkingSpace reserved = space(zoneId, "A-04", ParkingSpaceStatus.RESERVED, null);
        ParkingSpace maintenance = space(zoneId, "A-05", ParkingSpaceStatus.MAINTENANCE, null);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId, TrackingMode.SPACE)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of(available, visitor, registered, reserved, maintenance));
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setParkingSpaceId(reserved.getParkingSpaceId());
        when(allocationPortOut.findActiveAllocationsForSpacesOnDate(any(), any(LocalDate.class)))
                .thenReturn(List.of(allocation));

        SpaceSummaryResult result = useCase.getZoneSummary(zoneId);

        assertEquals(5, result.total());
        assertEquals(5, result.available() + result.occupied() + result.reserved() + result.maintenance());
        assertEquals(1, result.occupiedVisitor());
        assertEquals(1, result.occupiedRegistered());
        assertEquals(1, result.subscriptionAllocated());
    }

    @Test
    void shouldExcludeAllocatedAvailableSpaceFromAvailability() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingSpace available = space(zoneId, "A-01", ParkingSpaceStatus.AVAILABLE, null);
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setParkingSpaceId(available.getParkingSpaceId());
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId, TrackingMode.SPACE)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of(available));
        when(allocationPortOut.findActiveAllocationsForSpacesOnDate(any(), any(LocalDate.class)))
                .thenReturn(List.of(allocation));
        when(parkingSessionPortOut.findOpenByParkingSpaceIds(any())).thenReturn(List.of());

        SpaceSummaryResult result = useCase.getZoneSummary(zoneId);

        assertEquals(0, result.available());
        assertEquals(1, result.subscriptionAllocated());
    }
    @Test
    void shouldSummarizeCapacityZoneFromOpenSessions() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        Zone zone = zone(zoneId, lotId, TrackingMode.CAPACITY);
        zone.setCapacity(100);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(zonePortOut.countOpenSessions(zoneId)).thenReturn(30L);

        SpaceSummaryResult result = useCase.getZoneSummary(zoneId);

        assertEquals(100, result.total());
        assertEquals(70, result.available());
        assertEquals(30, result.occupied());
    }

    @Test
    void shouldFlagOccupiedWithoutSessionAndReservedWithoutAllocation() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingSpace occupied = space(zoneId, "A-01", ParkingSpaceStatus.OCCUPIED, ManualOccupancyType.VISITOR);
        ParkingSpace reserved = space(zoneId, "A-02", ParkingSpaceStatus.RESERVED, null);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId, TrackingMode.SPACE)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of(occupied, reserved));
        when(parkingSessionPortOut.findOpenByParkingSpaceId(any())).thenReturn(Optional.empty());
        when(allocationPortOut.findActiveAllocationsForSpaceOnDate(any(), any(LocalDate.class)))
                .thenReturn(List.of());

        var issues = useCase.findZoneReconciliationIssues(zoneId);

        assertTrue(issues.stream().anyMatch(issue ->
                "OCCUPIED_WITHOUT_OPEN_SESSION".equals(issue.issueType())));
        assertTrue(issues.stream().anyMatch(issue ->
                "RESERVED_WITHOUT_ALLOCATION".equals(issue.issueType())));
    }

    @Test
    void shouldFlagSessionTopologyMismatch() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingSpace occupied = space(zoneId, "A-01", ParkingSpaceStatus.OCCUPIED, ManualOccupancyType.VISITOR);
        ParkingSession session = new ParkingSession();
        session.setParkingSessionId(UUID.randomUUID());
        session.setZoneId(UUID.randomUUID());
        session.setParkingLotId(lotId);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId, TrackingMode.SPACE)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of(occupied));
        when(parkingSessionPortOut.findOpenByParkingSpaceId(occupied.getParkingSpaceId()))
                .thenReturn(Optional.of(session));
        when(allocationPortOut.findActiveAllocationsForSpaceOnDate(any(), any(LocalDate.class)))
                .thenReturn(List.of());

        var issues = useCase.findZoneReconciliationIssues(zoneId);

        assertTrue(issues.stream().anyMatch(issue ->
                "SESSION_SPACE_TOPOLOGY_MISMATCH".equals(issue.issueType())));
    }

    @Test
    void shouldDenySummaryOutsideScope() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId, TrackingMode.SPACE)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        doThrow(new AccessDeniedException("denied"))
                .when(organizationAccessGuard).ensureCanAccessParkingLot(any(ParkingLot.class));

        assertThrows(AccessDeniedException.class, () -> useCase.getZoneSummary(zoneId));
    }

    private Zone zone(UUID zoneId, UUID lotId, TrackingMode mode) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(lotId);
        zone.setCode("Z1");
        zone.setName("Zone 1");
        zone.setCapacity(0);
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setTrackingMode(mode);
        zone.setLayoutStatus(LayoutStatus.NOT_CONFIGURED);
        zone.setLayoutVersion(0L);
        return zone;
    }

    private ParkingLot lot(UUID lotId) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(UUID.randomUUID());
        return lot;
    }

    private ParkingSpace space(UUID zoneId, String code, ParkingSpaceStatus status,
            ManualOccupancyType occupancyType) {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(UUID.randomUUID());
        space.setZoneId(zoneId);
        space.setCode(code);
        space.setStatus(status);
        space.setLifecycleStatus("ACTIVE");
        space.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        space.setManualOccupancyType(occupancyType);
        space.setStatusReason("test");
        space.setVersion(0L);
        space.setRotation(BigDecimal.ZERO);
        return space;
    }
}
