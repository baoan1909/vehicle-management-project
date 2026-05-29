package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingLotPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ParkingLotPersistenceAdapterTest {

    @Mock
    private ParkingLotRepository parkingLotRepository;

    @Mock
    private ParkingLotPersistenceMapper parkingLotPersistenceMapper;

    @Mock
    private ZoneRepository zoneRepository;

    @InjectMocks
    private ParkingLotPersistenceAdapter parkingLotPersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingParkingLot() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(UUID.randomUUID());

        ParkingLotEntity parkingLotEntity = new ParkingLotEntity();

        when(parkingLotPersistenceMapper.toEntity(parkingLot)).thenReturn(parkingLotEntity);
        when(parkingLotRepository.saveAndFlush(parkingLotEntity)).thenReturn(parkingLotEntity);
        when(parkingLotPersistenceMapper.toDomain(parkingLotEntity)).thenReturn(parkingLot);

        ParkingLot savedParkingLot = parkingLotPersistenceAdapter.save(parkingLot);

        assertEquals(parkingLot, savedParkingLot);
        verify(parkingLotRepository).saveAndFlush(parkingLotEntity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLotEntity parkingLotEntity = new ParkingLotEntity();
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(parkingLotId);

        when(parkingLotRepository.findById(parkingLotId)).thenReturn(Optional.of(parkingLotEntity));
        when(parkingLotPersistenceMapper.toDomain(parkingLotEntity)).thenReturn(parkingLot);

        Optional<ParkingLot> result = parkingLotPersistenceAdapter.findById(parkingLotId);

        assertTrue(result.isPresent());
        assertEquals(parkingLotId, result.get().getParkingLotId());
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        ParkingLotEntity firstEntity = new ParkingLotEntity();
        ParkingLotEntity secondEntity = new ParkingLotEntity();
        ParkingLot firstParkingLot = new ParkingLot();
        ParkingLot secondParkingLot = new ParkingLot();

        when(parkingLotRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(parkingLotPersistenceMapper.toDomain(firstEntity)).thenReturn(firstParkingLot);
        when(parkingLotPersistenceMapper.toDomain(secondEntity)).thenReturn(secondParkingLot);

        List<ParkingLot> result = parkingLotPersistenceAdapter.findAll(ParkingLotStatus.ACTIVE, "HCMUTE");

        assertEquals(2, result.size());
        assertEquals(firstParkingLot, result.get(0));
        assertEquals(secondParkingLot, result.get(1));
    }

    @Test
    void shouldDelegateExistsByCode() {
        when(parkingLotRepository.existsByCode("HCMUTE")).thenReturn(true);

        boolean exists = parkingLotPersistenceAdapter.existsByCode("HCMUTE");

        assertTrue(exists);
        verify(parkingLotRepository).existsByCode("HCMUTE");
    }

    @Test
    void shouldDelegateExistsByCodeAndParkingLotIdNot() {
        UUID parkingLotId = UUID.randomUUID();

        when(parkingLotRepository.existsByCodeAndParkingLotIdNot("HCMUTE", parkingLotId)).thenReturn(true);

        boolean exists = parkingLotPersistenceAdapter.existsByCodeAndParkingLotIdNot("HCMUTE", parkingLotId);

        assertTrue(exists);
        verify(parkingLotRepository).existsByCodeAndParkingLotIdNot("HCMUTE", parkingLotId);
    }

    @Test
    void shouldCheckActiveZones() {
        UUID parkingLotId = UUID.randomUUID();

        when(zoneRepository.existsByParkingLotIdAndStatus(parkingLotId, ZoneStatus.ACTIVE)).thenReturn(true);

        boolean hasActiveZones = parkingLotPersistenceAdapter.hasActiveZones(parkingLotId);

        assertTrue(hasActiveZones);
        verify(zoneRepository).existsByParkingLotIdAndStatus(parkingLotId, ZoneStatus.ACTIVE);
    }
}
