package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.infrastructure.mapper.parking.GatePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.GateEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.GateRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
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
class GatePersistenceAdapterTest {

    @Mock
    private GateRepository gateRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private LaneRepository laneRepository;

    @Mock
    private GatePersistenceMapper gatePersistenceMapper;

    @InjectMocks
    private GatePersistenceAdapter gatePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingGate() {
        Gate gate = new Gate();
        gate.setGateId(UUID.randomUUID());

        GateEntity gateEntity = new GateEntity();

        when(gatePersistenceMapper.toEntity(gate)).thenReturn(gateEntity);
        when(gateRepository.saveAndFlush(gateEntity)).thenReturn(gateEntity);
        when(gatePersistenceMapper.toDomain(gateEntity)).thenReturn(gate);

        Gate savedGate = gatePersistenceAdapter.save(gate);

        assertEquals(gate, savedGate);
        verify(gateRepository).saveAndFlush(gateEntity);
    }

    @Test
    void shouldMapEntityWhenFindingById() {
        UUID gateId = UUID.randomUUID();
        GateEntity gateEntity = new GateEntity();
        Gate gate = new Gate();
        gate.setGateId(gateId);

        when(gateRepository.findById(gateId)).thenReturn(Optional.of(gateEntity));
        when(gatePersistenceMapper.toDomain(gateEntity)).thenReturn(gate);

        Optional<Gate> result = gatePersistenceAdapter.findById(gateId);

        assertTrue(result.isPresent());
        assertEquals(gateId, result.get().getGateId());
    }

    @Test
    void shouldReturnMappedListWhenFindingAll() {
        UUID zoneId = UUID.randomUUID();
        GateEntity firstEntity = new GateEntity();
        GateEntity secondEntity = new GateEntity();
        Gate firstGate = new Gate();
        Gate secondGate = new Gate();

        when(gateRepository.findAll(any(Specification.class))).thenReturn(List.of(firstEntity, secondEntity));
        when(gatePersistenceMapper.toDomain(firstEntity)).thenReturn(firstGate);
        when(gatePersistenceMapper.toDomain(secondEntity)).thenReturn(secondGate);

        List<Gate> result = gatePersistenceAdapter.findAll(zoneId, GateStatus.ACTIVE, "MOTO");

        assertEquals(2, result.size());
        assertEquals(firstGate, result.get(0));
        assertEquals(secondGate, result.get(1));
    }

    @Test
    void shouldCheckActiveZoneById() {
        UUID zoneId = UUID.randomUUID();

        when(zoneRepository.existsByZoneIdAndStatus(zoneId, ZoneStatus.ACTIVE)).thenReturn(true);

        boolean exists = gatePersistenceAdapter.existsActiveZoneById(zoneId);

        assertTrue(exists);
        verify(zoneRepository).existsByZoneIdAndStatus(zoneId, ZoneStatus.ACTIVE);
    }

    @Test
    void shouldDelegateExistsByZoneIdAndCode() {
        UUID zoneId = UUID.randomUUID();

        when(gateRepository.existsByZoneIdAndCode(zoneId, "MOTO-GATE-01")).thenReturn(true);

        boolean exists = gatePersistenceAdapter.existsByZoneIdAndCode(zoneId, "MOTO-GATE-01");

        assertTrue(exists);
        verify(gateRepository).existsByZoneIdAndCode(zoneId, "MOTO-GATE-01");
    }

    @Test
    void shouldDelegateExistsByZoneIdAndCodeAndGateIdNot() {
        UUID zoneId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();

        when(gateRepository.existsByZoneIdAndCodeAndGateIdNot(zoneId, "MOTO-GATE-01", gateId))
                .thenReturn(true);

        boolean exists = gatePersistenceAdapter.existsByZoneIdAndCodeAndGateIdNot(
                zoneId,
                "MOTO-GATE-01",
                gateId
        );

        assertTrue(exists);
        verify(gateRepository).existsByZoneIdAndCodeAndGateIdNot(zoneId, "MOTO-GATE-01", gateId);
    }

    @Test
    void shouldCheckActiveLanes() {
        UUID gateId = UUID.randomUUID();

        when(laneRepository.existsByGateIdAndStatus(gateId, LaneStatus.ACTIVE)).thenReturn(true);

        boolean hasActiveLanes = gatePersistenceAdapter.hasActiveLanes(gateId);

        assertTrue(hasActiveLanes);
        verify(laneRepository).existsByGateIdAndStatus(gateId, LaneStatus.ACTIVE);
    }
}
