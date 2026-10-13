package com.ban.vehicle_management.application.parking.parkingsummary.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.PublicAvailabilityResult;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicAvailabilityUseCaseTest {

    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private ParkingSpacePortOut parkingSpacePortOut;
    @Mock private ParkingSpaceAllocationPortOut allocationPortOut;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;

    @InjectMocks private PublicSpaceAvailabilityUseCaseImpl useCase;

    private final UUID lotId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID vehicleTypeId = UUID.randomUUID();

    @Test
    void shouldCountOnlyTrulyUsableSpaces() {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setName("Lot A");
        lot.setStatus(ParkingLotStatus.ACTIVE);
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(lotId);
        zone.setTrackingMode(TrackingMode.SPACE);
        UUID freeId = UUID.randomUUID();
        UUID occupiedId = UUID.randomUUID();
        UUID allocatedId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
        when(zonePortOut.findAll(lotId, null, null, null)).thenReturn(List.of(zone));
        when(parkingSpacePortOut.findByZoneIdAndLifecycleStatus(zoneId, "ACTIVE")).thenReturn(List.of(
                space(freeId, ParkingSpaceStatus.AVAILABLE),
                space(occupiedId, ParkingSpaceStatus.OCCUPIED),
                space(allocatedId, ParkingSpaceStatus.AVAILABLE),
                space(maintenanceId, ParkingSpaceStatus.MAINTENANCE)));
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setParkingSpaceId(allocatedId);
        when(allocationPortOut.findActiveAllocationsForSpacesOnDate(any(), any(LocalDate.class)))
                .thenReturn(List.of(allocation));
        ParkingSession open = new ParkingSession();
        open.setParkingSpaceId(occupiedId);
        when(parkingSessionPortOut.findOpenByParkingSpaceIds(any()))
                .thenReturn(List.of(open));

        PublicAvailabilityResult result = useCase.getPublicAvailability(lotId);

        assertEquals(4, result.total());
        assertEquals(1, result.visitorAvailable());
        assertEquals(1, result.subscriptionAvailable());
        assertTrue(result.hasAvailability());
        assertEquals(1, result.breakdown().size());
        assertEquals(1, result.breakdown().get(0).visitorAvailable());
    }

    @Test
    void shouldHideInactiveLots() {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setStatus(ParkingLotStatus.CLOSED);
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));

        assertThrows(NotFoundException.class, () -> useCase.getPublicAvailability(lotId));
    }

    private ParkingSpace space(UUID id, ParkingSpaceStatus status) {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(id);
        space.setZoneId(zoneId);
        space.setCode("S-" + id.toString().substring(0, 4));
        space.setVehicleTypeId(vehicleTypeId);
        space.setLifecycleStatus("ACTIVE");
        space.setStatus(status);
        return space;
    }
}
