package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.infrastructure.mapper.parking.GatePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.GateEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.GateRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.parking.GateSpecifications;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GatePersistenceAdapter implements GatePortOut {

    private final GateRepository gateRepository;
    private final ZoneRepository zoneRepository;
    private final LaneRepository laneRepository;
    private final GatePersistenceMapper gatePersistenceMapper;

    public GatePersistenceAdapter(
            GateRepository gateRepository,
            ZoneRepository zoneRepository,
            LaneRepository laneRepository,
            GatePersistenceMapper gatePersistenceMapper
    ) {
        this.gateRepository = gateRepository;
        this.zoneRepository = zoneRepository;
        this.laneRepository = laneRepository;
        this.gatePersistenceMapper = gatePersistenceMapper;
    }

    @Override
    public Gate save(Gate gate) {
        GateEntity savedEntity = gateRepository.saveAndFlush(gatePersistenceMapper.toEntity(gate));
        return gatePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Gate> findById(UUID gateId) {
        return gateRepository.findById(gateId)
                .map(gatePersistenceMapper::toDomain);
    }

    @Override
    public List<Gate> findAll(UUID zoneId, GateStatus status, String keyword) {
        return gateRepository.findAll(GateSpecifications.withFilters(zoneId, status, keyword))
                .stream()
                .map(gatePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveZoneById(UUID zoneId) {
        return zoneRepository.existsByZoneIdAndStatus(zoneId, ZoneStatus.ACTIVE);
    }

    @Override
    public boolean existsByZoneIdAndCode(UUID zoneId, String code) {
        return gateRepository.existsByZoneIdAndCode(zoneId, code);
    }

    @Override
    public boolean existsByZoneIdAndCodeAndGateIdNot(UUID zoneId, String code, UUID gateId) {
        return gateRepository.existsByZoneIdAndCodeAndGateIdNot(zoneId, code, gateId);
    }

    @Override
    public boolean hasActiveLanes(UUID gateId) {
        return laneRepository.existsByGateIdAndStatus(gateId, LaneStatus.ACTIVE);
    }
}