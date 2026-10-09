package com.ban.vehicle_management.application.parking.parkingspace.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
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
class ParkingSpaceUseCaseScopeTest {

    @Mock
    private ParkingSpacePortOut parkingSpacePortOut;
    @Mock
    private ZonePortOut zonePortOut;
    @Mock
    private ParkingLotPortOut parkingLotPortOut;
    @Mock
    private OrganizationAccessGuard organizationAccessGuard;

    @InjectMocks
    private ParkingSpaceUseCaseImpl useCase;

    @Test
    void shouldDenyReadWhenLotScopeForbidsIt() {
        UUID spaceId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(space(zoneId)));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        doThrow(new AccessDeniedException("denied"))
                .when(organizationAccessGuard).ensureCanAccessParkingLot(any(ParkingLot.class));

        assertThrows(AccessDeniedException.class, () -> useCase.getParkingSpaceById(spaceId));
    }

    @Test
    void shouldDenyWriteWhenLotCannotBeConfigured() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        doThrow(new AccessDeniedException("denied"))
                .when(organizationAccessGuard).ensureCanConfigureParkingLot(any(ParkingLot.class));

        assertThrows(AccessDeniedException.class,
                () -> useCase.createParkingSpace(newSpace(zoneId, "B-01")));
        verify(parkingSpacePortOut, never()).save(any(ParkingSpace.class));
    }

    @Test
    void shouldResolveScopeFromStoredZoneNotRequestPayload() {
        UUID spaceId = UUID.randomUUID();
        UUID storedZoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingSpace existing = space(storedZoneId);
        existing.setParkingSpaceId(spaceId);
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(existing));
        when(zonePortOut.findById(storedZoneId)).thenReturn(Optional.of(zone(storedZoneId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));

        ParkingSpace request = space(UUID.randomUUID());
        request.setCode("B-02");

        assertThrows(BadRequestException.class, () -> useCase.updateParkingSpace(spaceId, request));
        verify(parkingSpacePortOut, never()).save(any(ParkingSpace.class));
    }

    @Test
    void shouldArchiveInsteadOfHardDelete() {
        UUID spaceId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingSpace existing = space(zoneId);
        existing.setParkingSpaceId(spaceId);
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(existing));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(parkingSpacePortOut.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.deleteParkingSpace(spaceId);

        verify(parkingSpacePortOut).save(any(ParkingSpace.class));
        assertEquals("ARCHIVED", existing.getLifecycleStatus());
    }

    @Test
    void shouldReturnFullBulkResultWithGeneratedIds() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(parkingSpacePortOut.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ParkingSpace> created = useCase.bulkCreateParkingSpaces(
                List.of(newSpace(zoneId, "B-01"), newSpace(zoneId, "B-02")));

        assertEquals(2, created.size());
    }

    @Test
    void shouldHonorStatusFilter() {
        UUID zoneId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingSpace available = space(zoneId);
        ParkingSpace occupied = space(zoneId);
        occupied.setStatus(ParkingSpaceStatus.OCCUPIED);
        occupied.setManualOccupancyType(ManualOccupancyType.VISITOR);
        occupied.setStatusReason("manual");
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone(zoneId, lotId)));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot(lotId)));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE"))
                .thenReturn(List.of(available, occupied));

        List<ParkingSpace> result = useCase.getParkingSpaces(zoneId, ParkingSpaceStatus.AVAILABLE, null);

        assertEquals(1, result.size());
        assertEquals(ParkingSpaceStatus.AVAILABLE, result.get(0).getStatus());
    }

    private ParkingSpace space(UUID zoneId) {
        return newSpace(zoneId, "B-01");
    }

    private ParkingSpace newSpace(UUID zoneId, String code) {
        ParkingSpace space = new ParkingSpace();
        space.setZoneId(zoneId);
        space.setCode(code);
        space.setStatus(ParkingSpaceStatus.AVAILABLE);
        space.setLifecycleStatus("ACTIVE");
        space.setStatusSource(ParkingSpaceStatusSource.MANUAL);
        space.setVersion(0L);
        space.setRotation(java.math.BigDecimal.ZERO);
        return space;
    }

    private Zone zone(UUID zoneId, UUID lotId) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(lotId);
        zone.setCode("Z1");
        zone.setName("Zone 1");
        zone.setCapacity(10);
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setTrackingMode(TrackingMode.SPACE);
        zone.setLayoutStatus(LayoutStatus.DRAFT);
        zone.setLayoutVersion(1L);
        return zone;
    }

    private ParkingLot lot(UUID lotId) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(UUID.randomUUID());
        return lot;
    }
}
