package com.ban.vehicle_management.application.parking.zone.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZoneUseCaseImplTest {

    @Mock
    private ZonePortOut zonePortOut;

    @InjectMocks
    private ZoneUseCaseImpl zoneUseCase;

    @Test
    void shouldCreateZoneWhenValid() {
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Zone request = validZone(parkingLotId, vehicleTypeId);

        when(zonePortOut.existsActiveParkingLotById(parkingLotId)).thenReturn(true);
        when(zonePortOut.existsActiveVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(zonePortOut.existsByParkingLotIdAndCode(parkingLotId, "A1")).thenReturn(false);
        when(zonePortOut.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Zone createdZone = zoneUseCase.createZone(request);

        assertNotNull(createdZone.getZoneId());
        assertEquals("A1", createdZone.getCode());
        assertEquals(ZoneStatus.ACTIVE, createdZone.getStatus());
    }

    @Test
    void shouldRejectCreateWhenParkingLotIsNotActive() {
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Zone request = validZone(parkingLotId, vehicleTypeId);

        when(zonePortOut.existsActiveParkingLotById(parkingLotId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> zoneUseCase.createZone(request));
        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldRejectCreateWhenVehicleTypeIsNotActive() {
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Zone request = validZone(parkingLotId, vehicleTypeId);

        when(zonePortOut.existsActiveParkingLotById(parkingLotId)).thenReturn(true);
        when(zonePortOut.existsActiveVehicleTypeById(vehicleTypeId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> zoneUseCase.createZone(request));
        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldRejectCreateWhenCodeAlreadyExistsInParkingLot() {
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Zone request = validZone(parkingLotId, vehicleTypeId);

        when(zonePortOut.existsActiveParkingLotById(parkingLotId)).thenReturn(true);
        when(zonePortOut.existsActiveVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(zonePortOut.existsByParkingLotIdAndCode(parkingLotId, "A1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> zoneUseCase.createZone(request));
        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldReturnZoneById() {
        UUID zoneId = UUID.randomUUID();
        Zone existingZone = validZone(UUID.randomUUID(), UUID.randomUUID());
        existingZone.setZoneId(zoneId);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));

        Zone result = zoneUseCase.getZoneById(zoneId);

        assertEquals(zoneId, result.getZoneId());
    }

    @Test
    void shouldThrowWhenZoneDoesNotExist() {
        UUID zoneId = UUID.randomUUID();

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> zoneUseCase.getZoneById(zoneId));
    }

    @Test
    void shouldReturnFilteredZonesWithTrimmedKeyword() {
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();

        when(zonePortOut.findAll(parkingLotId, vehicleTypeId, ZoneStatus.ACTIVE, "A1"))
                .thenReturn(List.of(new Zone(), new Zone()));

        List<Zone> zones = zoneUseCase.getZones(parkingLotId, vehicleTypeId, ZoneStatus.ACTIVE, " A1 ");

        assertEquals(2, zones.size());
        verify(zonePortOut).findAll(parkingLotId, vehicleTypeId, ZoneStatus.ACTIVE, "A1");
    }

    @Test
    void shouldUpdateZoneWhenValid() {
        UUID zoneId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Zone existingZone = validZone(parkingLotId, vehicleTypeId);
        existingZone.setZoneId(zoneId);
        existingZone.setStatus(ZoneStatus.MAINTENANCE);

        Zone request = new Zone();
        request.setCode(" b1 ");
        request.setName(" Zone B1 ");
        request.setVehicleTypeId(vehicleTypeId);
        request.setCapacity(120);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));
        when(zonePortOut.existsActiveVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(zonePortOut.countOpenSessions(zoneId)).thenReturn(10L);
        when(zonePortOut.existsByParkingLotIdAndCodeAndZoneIdNot(parkingLotId, "B1", zoneId)).thenReturn(false);
        when(zonePortOut.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Zone updatedZone = zoneUseCase.updateZone(zoneId, request);

        assertEquals("B1", updatedZone.getCode());
        assertEquals("Zone B1", updatedZone.getName());
        assertEquals(120, updatedZone.getCapacity());
        assertEquals(ZoneStatus.MAINTENANCE, updatedZone.getStatus());
    }

    @Test
    void shouldRejectUpdateWhenCapacityIsLessThanOpenSessions() {
        UUID zoneId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Zone existingZone = validZone(parkingLotId, vehicleTypeId);
        existingZone.setZoneId(zoneId);

        Zone request = new Zone();
        request.setCode("A1");
        request.setName("Area A1");
        request.setVehicleTypeId(vehicleTypeId);
        request.setCapacity(1);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));
        when(zonePortOut.existsActiveVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(zonePortOut.countOpenSessions(zoneId)).thenReturn(2L);

        assertThrows(BadRequestException.class, () -> zoneUseCase.updateZone(zoneId, request));
        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldRejectUpdateWhenCodeAlreadyExistsInParkingLot() {
        UUID zoneId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Zone existingZone = validZone(parkingLotId, vehicleTypeId);
        existingZone.setZoneId(zoneId);

        Zone request = validZone(parkingLotId, vehicleTypeId);
        request.setCode("B1");

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));
        when(zonePortOut.existsActiveVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(zonePortOut.countOpenSessions(zoneId)).thenReturn(0L);
        when(zonePortOut.existsByParkingLotIdAndCodeAndZoneIdNot(parkingLotId, "B1", zoneId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> zoneUseCase.updateZone(zoneId, request));
        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldCloseZoneOnDelete() {
        UUID zoneId = UUID.randomUUID();
        Zone existingZone = validZone(UUID.randomUUID(), UUID.randomUUID());
        existingZone.setZoneId(zoneId);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));
        when(zonePortOut.hasOpenSessions(zoneId)).thenReturn(false);

        zoneUseCase.deleteZone(zoneId);

        assertEquals(ZoneStatus.CLOSED, existingZone.getStatus());
        verify(zonePortOut).save(existingZone);
    }

    @Test
    void shouldDoNothingWhenDeletingClosedZone() {
        UUID zoneId = UUID.randomUUID();
        Zone existingZone = validZone(UUID.randomUUID(), UUID.randomUUID());
        existingZone.setZoneId(zoneId);
        existingZone.setStatus(ZoneStatus.CLOSED);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));

        zoneUseCase.deleteZone(zoneId);

        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldRejectDeleteWhenZoneHasOpenSessions() {
        UUID zoneId = UUID.randomUUID();
        Zone existingZone = validZone(UUID.randomUUID(), UUID.randomUUID());
        existingZone.setZoneId(zoneId);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));
        when(zonePortOut.hasOpenSessions(zoneId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> zoneUseCase.deleteZone(zoneId));
        verify(zonePortOut, never()).save(any(Zone.class));
    }

    @Test
    void shouldActivateZone() {
        UUID zoneId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Zone existingZone = validZone(parkingLotId, vehicleTypeId);
        existingZone.setZoneId(zoneId);
        existingZone.setStatus(ZoneStatus.CLOSED);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));
        when(zonePortOut.existsActiveParkingLotById(parkingLotId)).thenReturn(true);
        when(zonePortOut.existsActiveVehicleTypeById(vehicleTypeId)).thenReturn(true);
        when(zonePortOut.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Zone activatedZone = zoneUseCase.activateZone(zoneId);

        assertEquals(ZoneStatus.ACTIVE, activatedZone.getStatus());
    }

    @Test
    void shouldMarkZoneMaintenance() {
        UUID zoneId = UUID.randomUUID();
        Zone existingZone = validZone(UUID.randomUUID(), UUID.randomUUID());
        existingZone.setZoneId(zoneId);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));
        when(zonePortOut.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Zone zone = zoneUseCase.markZoneMaintenance(zoneId);

        assertEquals(ZoneStatus.MAINTENANCE, zone.getStatus());
    }

    @Test
    void shouldCloseZone() {
        UUID zoneId = UUID.randomUUID();
        Zone existingZone = validZone(UUID.randomUUID(), UUID.randomUUID());
        existingZone.setZoneId(zoneId);

        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(existingZone));
        when(zonePortOut.hasOpenSessions(zoneId)).thenReturn(false);
        when(zonePortOut.save(any(Zone.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Zone zone = zoneUseCase.closeZone(zoneId);

        assertEquals(ZoneStatus.CLOSED, zone.getStatus());
    }

    private Zone validZone(UUID parkingLotId, UUID vehicleTypeId) {
        Zone zone = new Zone();
        zone.setParkingLotId(parkingLotId);
        zone.setVehicleTypeId(vehicleTypeId);
        zone.setCode("A1");
        zone.setName("Area A1");
        zone.setCapacity(100);
        zone.setStatus(ZoneStatus.ACTIVE);
        return zone;
    }
}
