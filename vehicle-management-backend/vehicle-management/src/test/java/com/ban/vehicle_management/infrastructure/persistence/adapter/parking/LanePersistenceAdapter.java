package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.infrastructure.mapper.parking.LanePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.GateRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
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
class LanePersistenceAdapterTest {

    @Mock
    private LaneRepository laneRepository;

    @Mock
    private GateRepository gateRepository;

    @Mock
    private ParkingSessionRepository parkingSessionRepository;

    @Mock
    private LanePersistenceMapper lanePersistenceMapper;

    @InjectMocks
    private LanePersistenceAdapter lanePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingLane() {
        Lane lane = new Lane();
        LaneEntity entity = new LaneEntity();

        when(lanePersistenceMapper.toEntity(lane)).thenReturn(entity);
        when(laneRepository.saveAndFlush(entity)).thenReturn(entity);
        when(lanePersistenceMapper.toDomain(entity)).thenReturn(lane);

        Lane savedLane = lanePersistenceAdapter.save(lane);

        assertEquals(lane, savedLane);
        verify(laneRepository).saveAndFlush(entity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID laneId = UUID.randomUUID();
        LaneEntity entity = new LaneEntity();
        Lane lane = new Lane();
        lane.setLaneId(laneId);

        when(laneRepository.findById(laneId)).thenReturn(Optional.of(entity));
        when(lanePersistenceMapper.toDomain(entity)).thenReturn(lane);

        Optional<Lane> result = lanePersistenceAdapter.findById(laneId);

        assertTrue(result.isPresent());
        assertEquals(laneId, result.get().getLaneId());
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        LaneEntity firstEntity = new LaneEntity();
        LaneEntity secondEntity = new LaneEntity();

        Lane firstLane = new Lane();
        Lane secondLane = new Lane();

        when(laneRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(lanePersistenceMapper.toDomain(firstEntity)).thenReturn(firstLane);
        when(lanePersistenceMapper.toDomain(secondEntity)).thenReturn(secondLane);

        List<Lane> result = lanePersistenceAdapter.findAll(
                UUID.randomUUID(),
                LaneDirection.OUT,
                LaneStatus.ACTIVE,
                "OUT"
        );

        assertEquals(2, result.size());
        assertEquals(firstLane, result.get(0));
        assertEquals(secondLane, result.get(1));
    }

    @Test
    void shouldCheckOperationalGateById() {
        UUID gateId = UUID.randomUUID();

        when(gateRepository.existsOperationalGateById(gateId)).thenReturn(true);

        boolean result = lanePersistenceAdapter.existsOperationalGateById(gateId);

        assertTrue(result);
        verify(gateRepository).existsOperationalGateById(gateId);
    }

    @Test
    void shouldDelegateExistsByGateIdAndCode() {
        UUID gateId = UUID.randomUUID();

        when(laneRepository.existsByGateIdAndCode(gateId, "LANE-IN-01")).thenReturn(true);

        boolean result = lanePersistenceAdapter.existsByGateIdAndCode(gateId, "LANE-IN-01");

        assertTrue(result);
        verify(laneRepository).existsByGateIdAndCode(gateId, "LANE-IN-01");
    }

    @Test
    void shouldDelegateExistsByGateIdAndCodeAndLaneIdNot() {
        UUID gateId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();

        when(laneRepository.existsByGateIdAndCodeAndLaneIdNot(gateId, "LANE-IN-01", laneId))
                .thenReturn(true);

        boolean result = lanePersistenceAdapter.existsByGateIdAndCodeAndLaneIdNot(
                gateId,
                "LANE-IN-01",
                laneId
        );

        assertTrue(result);
        verify(laneRepository).existsByGateIdAndCodeAndLaneIdNot(gateId, "LANE-IN-01", laneId);
    }

    @Test
    void shouldFindZoneIdByGateId() {
        UUID gateId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();

        when(gateRepository.findZoneIdByGateId(gateId)).thenReturn(Optional.of(zoneId));

        Optional<UUID> result = lanePersistenceAdapter.findZoneIdByGateId(gateId);

        assertTrue(result.isPresent());
        assertEquals(zoneId, result.get());
    }

    @Test
    void shouldCheckOpenSessionsInZone() {
        UUID zoneId = UUID.randomUUID();

        when(parkingSessionRepository.existsByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN))
                .thenReturn(true);

        boolean result = lanePersistenceAdapter.hasOpenSessionsInZone(zoneId);

        assertTrue(result);
        verify(parkingSessionRepository).existsByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN);
    }

    @Test
    void shouldCheckOtherActiveOutLaneInZone() {
        UUID zoneId = UUID.randomUUID();
        UUID excludedLaneId = UUID.randomUUID();

        when(laneRepository.existsOtherLaneInZoneByStatusAndDirection(
                zoneId,
                excludedLaneId,
                LaneStatus.ACTIVE,
                LaneDirection.OUT
        )).thenReturn(true);

        boolean result = lanePersistenceAdapter.hasOtherActiveOutLaneInZone(zoneId, excludedLaneId);

        assertTrue(result);
        verify(laneRepository).existsOtherLaneInZoneByStatusAndDirection(
                zoneId,
                excludedLaneId,
                LaneStatus.ACTIVE,
                LaneDirection.OUT
        );
    }
}