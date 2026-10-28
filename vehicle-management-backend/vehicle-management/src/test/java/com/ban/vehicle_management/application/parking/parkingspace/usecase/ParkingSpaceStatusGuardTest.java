package com.ban.vehicle_management.application.parking.parkingspace.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceStatusGuardTest {

    @Mock private ParkingSpacePortOut parkingSpacePortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;
    @Mock private ParkingSpaceAllocationPortOut allocationPortOut;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private AuditLogPortOut auditLogPortOut;
    @Mock private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks private ParkingSpaceUseCaseImpl useCase;

    @Test
    void shouldBlockClearingOccupiedSpaceWithOpenSession() {
        UUID spaceId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        when(parkingSpacePortOut.findById(spaceId))
                .thenReturn(Optional.of(occupiedSpace(spaceId, zoneId)));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(parkingLotPortOut.findById(any())).thenReturn(Optional.of(lot()));
        when(parkingSessionPortOut.existsOpenByParkingSpaceId(spaceId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.setManualStatus(
                spaceId, ParkingSpaceStatus.AVAILABLE, null, "cleaned"));
    }

    @Test
    void shouldBlockMaintenanceWithActiveAllocation() {
        UUID spaceId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        when(parkingSpacePortOut.findById(spaceId))
                .thenReturn(Optional.of(availableSpace(spaceId, zoneId)));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(parkingLotPortOut.findById(any())).thenReturn(Optional.of(lot()));
        when(parkingSessionPortOut.existsOpenByParkingSpaceId(spaceId)).thenReturn(false);
        var allocation = new com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation();
        allocation.setAllocationStatus(
                com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus.ACTIVE);
        when(allocationPortOut.findByParkingSpaceId(spaceId)).thenReturn(List.of(allocation));

        assertThrows(ConflictException.class, () -> useCase.setManualStatus(
                spaceId, ParkingSpaceStatus.MAINTENANCE, null, "repair floor"));
    }

    @Test
    void shouldRequireReasonForManualTransition() {
        UUID spaceId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        when(parkingSpacePortOut.findById(spaceId))
                .thenReturn(Optional.of(availableSpace(spaceId, zoneId)));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(parkingLotPortOut.findById(any())).thenReturn(Optional.of(lot()));
        when(parkingSessionPortOut.existsOpenByParkingSpaceId(spaceId)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> useCase.setManualStatus(
                spaceId, ParkingSpaceStatus.RESERVED, null, "  "));
    }

    @Test
    void shouldOccupyWithVisitorTypeAndAudit() {
        UUID spaceId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(parkingSpacePortOut.findById(spaceId))
                .thenReturn(Optional.of(availableSpace(spaceId, zoneId)));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(parkingLotPortOut.findById(any())).thenReturn(Optional.of(lot()));
        when(parkingSpacePortOut.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);

        ParkingSpace updated = useCase.setManualStatus(
                spaceId, ParkingSpaceStatus.OCCUPIED, ManualOccupancyType.VISITOR, "walk-in car");

        assertEquals(ParkingSpaceStatus.OCCUPIED, updated.getStatus());
        assertEquals(ManualOccupancyType.VISITOR, updated.getManualOccupancyType());
        verify(auditLogPortOut).save(any());
    }

    @Test
    void shouldAtomicallyReassignAllocationBeforeMaintenance() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        ParkingSpace source = availableSpace(sourceId, zoneId);
        ParkingSpace target = availableSpace(targetId, zoneId);
        target.setCode("A-02");
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setAllocationId(UUID.randomUUID());
        allocation.setParkingSpaceId(sourceId);
        allocation.setSubscriptionId(UUID.randomUUID());
        allocation.setEffectiveFrom(java.time.LocalDate.now());
        allocation.setEffectiveTo(java.time.LocalDate.now().plusDays(30));
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.ACTIVE);
        when(parkingSpacePortOut.findById(sourceId)).thenReturn(Optional.of(source));
        when(parkingSpacePortOut.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
        when(parkingSpacePortOut.findByIdForUpdate(targetId)).thenReturn(Optional.of(target));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId)));
        when(parkingLotPortOut.findById(any())).thenReturn(Optional.of(lot()));
        when(parkingSessionPortOut.existsOpenByParkingSpaceId(any())).thenReturn(false);
        when(allocationPortOut.findByParkingSpaceId(sourceId)).thenReturn(List.of(allocation));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of(source, target));
        when(allocationPortOut.existsOverlappingAllocation(
                any(), any(), any(), org.mockito.ArgumentMatchers.isNull())).thenReturn(false);
        when(allocationPortOut.save(any(ParkingSpaceAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSpacePortOut.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());

        ParkingSpace updated = useCase.setManualStatus(
                sourceId, ParkingSpaceStatus.MAINTENANCE, null, "repair floor");

        assertEquals(ParkingSpaceStatus.MAINTENANCE, updated.getStatus());
        assertEquals(ParkingSpaceStatus.RESERVED, target.getStatus());
        assertEquals(targetId, allocation.getParkingSpaceId());
        verify(allocationPortOut).save(allocation);
    }

    private ParkingSpace base(UUID spaceId, UUID zoneId, ParkingSpaceStatus status) {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(spaceId);
        space.setZoneId(zoneId);
        space.setCode("A-01");
        space.setStatus(status);
        space.setLifecycleStatus("ACTIVE");
        space.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        space.setStatusReason("test");
        space.setVersion(0L);
        space.setRotation(BigDecimal.ZERO);
        return space;
    }

    private ParkingSpace occupiedSpace(UUID spaceId, UUID zoneId) {
        ParkingSpace space = base(spaceId, zoneId, ParkingSpaceStatus.OCCUPIED);
        space.setManualOccupancyType(ManualOccupancyType.VISITOR);
        return space;
    }

    private ParkingSpace availableSpace(UUID spaceId, UUID zoneId) {
        return base(spaceId, zoneId, ParkingSpaceStatus.AVAILABLE);
    }

    private Zone zone(UUID zoneId) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(UUID.randomUUID());
        zone.setCode("Z1");
        zone.setName("Zone 1");
        zone.setCapacity(10);
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setTrackingMode(TrackingMode.SPACE);
        zone.setLayoutStatus(LayoutStatus.DRAFT);
        zone.setLayoutVersion(0L);
        return zone;
    }

    private ParkingLot lot() {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(UUID.randomUUID());
        lot.setOrganizationId(UUID.randomUUID());
        return lot;
    }
}
