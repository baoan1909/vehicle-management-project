package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.infrastructure.mapper.parking.ZonePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.GateRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ZonePersistenceAdapterTest {

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private ParkingLotRepository parkingLotRepository;

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    @Mock
    private ParkingSessionRepository parkingSessionRepository;

    @Mock
    private ZonePersistenceMapper zonePersistenceMapper;

    @InjectMocks
    private ZonePersistenceAdapter zonePersistenceAdapter;

    @Mock
    private GateRepository gateRepository;

    @Test
    void shouldUseSaveAndFlushWhenSavingZone() {
        Zone zone = new Zone();
        zone.setZoneId(UUID.randomUUID());

        ZoneEntity zoneEntity = new ZoneEntity();

        when(zonePersistenceMapper.toEntity(zone)).thenReturn(zoneEntity);
        when(zoneRepository.saveAndFlush(zoneEntity)).thenReturn(zoneEntity);
        when(zonePersistenceMapper.toDomain(zoneEntity)).thenReturn(zone);

        Zone savedZone = zonePersistenceAdapter.save(zone);

        assertEquals(zone, savedZone);
        verify(zoneRepository).saveAndFlush(zoneEntity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID zoneId = UUID.randomUUID();
        ZoneEntity zoneEntity = new ZoneEntity();
        Zone zone = new Zone();
        zone.setZoneId(zoneId);

        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(zoneEntity));
        when(zonePersistenceMapper.toDomain(zoneEntity)).thenReturn(zone);

        Optional<Zone> result = zonePersistenceAdapter.findById(zoneId);

        assertTrue(result.isPresent());
        assertEquals(zoneId, result.get().getZoneId());
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        ZoneEntity firstEntity = new ZoneEntity();
        ZoneEntity secondEntity = new ZoneEntity();
        Zone firstZone = new Zone();
        Zone secondZone = new Zone();

        when(zoneRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(zonePersistenceMapper.toDomain(firstEntity)).thenReturn(firstZone);
        when(zonePersistenceMapper.toDomain(secondEntity)).thenReturn(secondZone);

        List<Zone> result = zonePersistenceAdapter.findAll(
                parkingLotId,
                vehicleTypeId,
                ZoneStatus.ACTIVE,
                "A1"
        );

        assertEquals(2, result.size());
        assertEquals(firstZone, result.get(0));
        assertEquals(secondZone, result.get(1));
    }

    @Test
    void shouldDelegateExistsByParkingLotIdAndCode() {
        UUID parkingLotId = UUID.randomUUID();

        when(zoneRepository.existsByParkingLotIdAndCode(parkingLotId, "A1")).thenReturn(true);

        boolean exists = zonePersistenceAdapter.existsByParkingLotIdAndCode(parkingLotId, "A1");

        assertTrue(exists);
        verify(zoneRepository).existsByParkingLotIdAndCode(parkingLotId, "A1");
    }

    @Test
    void shouldDelegateExistsByParkingLotIdAndCodeAndZoneIdNot() {
        UUID parkingLotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();

        when(zoneRepository.existsByParkingLotIdAndCodeAndZoneIdNot(parkingLotId, "A1", zoneId))
                .thenReturn(true);

        boolean exists = zonePersistenceAdapter.existsByParkingLotIdAndCodeAndZoneIdNot(
                parkingLotId,
                "A1",
                zoneId
        );

        assertTrue(exists);
        verify(zoneRepository).existsByParkingLotIdAndCodeAndZoneIdNot(parkingLotId, "A1", zoneId);
    }

    @Test
    void shouldCheckActiveParkingLotById() {
        UUID parkingLotId = UUID.randomUUID();

        when(parkingLotRepository.existsByParkingLotIdAndStatus(parkingLotId, ParkingLotStatus.ACTIVE))
                .thenReturn(true);

        boolean exists = zonePersistenceAdapter.existsActiveParkingLotById(parkingLotId);

        assertTrue(exists);
        verify(parkingLotRepository).existsByParkingLotIdAndStatus(parkingLotId, ParkingLotStatus.ACTIVE);
    }

    @Test
    void shouldReturnTrueWhenVehicleTypeIdIsNull() {
        boolean exists = zonePersistenceAdapter.existsActiveVehicleTypeById(null);

        assertTrue(exists);
        verify(vehicleTypeRepository, never()).existsByVehicleTypeIdAndIsActiveTrue(any());
    }

    @Test
    void shouldCheckActiveVehicleTypeById() {
        UUID vehicleTypeId = UUID.randomUUID();

        when(vehicleTypeRepository.existsByVehicleTypeIdAndIsActiveTrue(vehicleTypeId)).thenReturn(true);

        boolean exists = zonePersistenceAdapter.existsActiveVehicleTypeById(vehicleTypeId);

        assertTrue(exists);
        verify(vehicleTypeRepository).existsByVehicleTypeIdAndIsActiveTrue(vehicleTypeId);
    }

    @Test
    void shouldCountOpenSessions() {
        UUID zoneId = UUID.randomUUID();

        when(parkingSessionRepository.countByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN)).thenReturn(3L);

        long openSessions = zonePersistenceAdapter.countOpenSessions(zoneId);

        assertEquals(3L, openSessions);
        verify(parkingSessionRepository).countByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN);
    }

    @Test
    void shouldCheckOpenSessions() {
        UUID zoneId = UUID.randomUUID();

        when(parkingSessionRepository.existsByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN)).thenReturn(true);

        boolean hasOpenSessions = zonePersistenceAdapter.hasOpenSessions(zoneId);

        assertTrue(hasOpenSessions);
        verify(parkingSessionRepository).existsByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN);
    }

    @Test
    void shouldCheckActiveGates() {
        UUID zoneId = UUID.randomUUID();

        when(gateRepository.existsByZoneIdAndStatus(zoneId, GateStatus.ACTIVE)).thenReturn(true);

        boolean hasActiveGates = zonePersistenceAdapter.hasActiveGates(zoneId);

        assertTrue(hasActiveGates);
        verify(gateRepository).existsByZoneIdAndStatus(zoneId, GateStatus.ACTIVE);
    }
}
