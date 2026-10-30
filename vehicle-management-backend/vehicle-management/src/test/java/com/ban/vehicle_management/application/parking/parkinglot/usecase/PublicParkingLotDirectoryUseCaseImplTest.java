package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.availability.port.in.PublicParkingLotCatalogPortIn;
import com.ban.vehicle_management.application.parking.availability.ParkingAvailabilityQuery;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.parking.availability.model.ParkingAvailabilitySnapshot;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicParkingLotDirectoryUseCaseImplTest {

    private static final UUID ACTIVE_LOT_ID = UUID.randomUUID();
    private static final UUID SETUP_LOT_ID = UUID.randomUUID();
    private static final UUID VEHICLE_TYPE_ID = UUID.randomUUID();

    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private PublicParkingLotCatalogPortIn publicParkingLotCatalogPortIn;
    @Mock private ParkingAvailabilityQuery parkingAvailabilityQuery;
    @InjectMocks private PublicParkingLotDirectoryUseCaseImpl useCase;

    @Test
    void directoryIncludesVisibleStatusesAndReportsAvailabilityOnlyForActiveLots() {
        ParkingLot activeLot = lot(ACTIVE_LOT_ID, "Active lot", ParkingLotStatus.ACTIVE);
        ParkingLot setupLot = lot(SETUP_LOT_ID, "Setup lot", ParkingLotStatus.SETUP);
        Zone activeZone = new Zone();
        activeZone.setZoneId(UUID.randomUUID());
        activeZone.setVehicleTypeId(VEHICLE_TYPE_ID);

        when(parkingLotPortOut.findAll(ParkingLotStatus.ACTIVE, null, null, null))
                .thenReturn(List.of(activeLot));
        when(parkingLotPortOut.findAll(ParkingLotStatus.SETUP, null, null, null))
                .thenReturn(List.of(setupLot));
        when(parkingLotPortOut.findAll(ParkingLotStatus.MAINTENANCE, null, null, null))
                .thenReturn(List.of());
        when(zonePortOut.findAll(ACTIVE_LOT_ID, null, ZoneStatus.ACTIVE, null))
                .thenReturn(List.of(activeZone));
        when(zonePortOut.findAll(SETUP_LOT_ID, null, ZoneStatus.ACTIVE, null))
                .thenReturn(List.of());
        when(publicParkingLotCatalogPortIn.getVehicleTypes(ACTIVE_LOT_ID))
                .thenReturn(List.of(vehicleType()));
        when(publicParkingLotCatalogPortIn.getVehicleTypes(SETUP_LOT_ID))
                .thenReturn(List.of());
        when(parkingAvailabilityQuery.get(any(Zone.class), any()))
                .thenReturn(new ParkingAvailabilitySnapshot(
                        activeZone.getZoneId(), 10, 0, 10, 2, 1, 2,
                        7, 5, 7, 2, 1, 0));

        var results = useCase.getDirectory();

        assertEquals(List.of(ACTIVE_LOT_ID, SETUP_LOT_ID),
                results.stream().map(item -> item.parkingLotId()).toList());
        assertEquals(10, results.getFirst().totalCapacity());
        assertEquals(5, results.getFirst().visitorAvailable());
        assertTrue(results.getFirst().hasAvailability());
        assertEquals(1, results.getFirst().vehicleTypes().size());
        assertEquals(0, results.get(1).visitorAvailable());
        assertFalse(results.get(1).hasAvailability());
        verify(parkingLotPortOut, never()).findAll(ParkingLotStatus.CLOSED, null, null, null);
    }

    private ParkingLot lot(UUID id, String name, ParkingLotStatus status) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(id);
        lot.setCode("LOT-" + id.toString().substring(0, 6));
        lot.setName(name);
        lot.setStatus(status);
        return lot;
    }

    private VehicleType vehicleType() {
        VehicleType type = new VehicleType();
        type.setVehicleTypeId(VEHICLE_TYPE_ID);
        type.setCode("MOTORBIKE");
        type.setName("Xe máy");
        return type;
    }
}
