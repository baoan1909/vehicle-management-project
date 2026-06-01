package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.infrastructure.mapper.parking.LanePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.GateRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.parking.LaneSpecifications;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LanePersistenceAdapter implements LanePortOut {

    private final LaneRepository laneRepository;
    private  final GateRepository gateRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private  final LanePersistenceMapper lanePersistenceMapper;

    public LanePersistenceAdapter(
            LaneRepository laneRepository,
            GateRepository gateRepository,
            ParkingSessionRepository parkingSessionRepository,
            LanePersistenceMapper lanePersistenceMapper
    ){
        this.laneRepository = laneRepository;
        this.gateRepository = gateRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.lanePersistenceMapper = lanePersistenceMapper;
    }

    @Override
    public Lane save(Lane lane){
        LaneEntity savedEntity = laneRepository.saveAndFlush(lanePersistenceMapper.toEntity(lane));
        return  lanePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Lane> findById(UUID laneId){
        return laneRepository.findById(laneId).map(lanePersistenceMapper::toDomain);
    }

    @Override
    public List<Lane> findAll(UUID gateId, LaneDirection direction, LaneStatus status, String keyword) {
        return laneRepository.findAll(LaneSpecifications.withFilters(gateId, direction, status, keyword))
                .stream()
                .map(lanePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsOperationalGateById(UUID gateId){
        return gateRepository.existsOperationalGateById(gateId);
    }

    @Override
    public boolean existsByGateIdAndCode(UUID gateId, String code) {
        return laneRepository.existsByGateIdAndCode(gateId, code);
    }

    @Override
    public boolean existsByGateIdAndCodeAndLaneIdNot(UUID gateId, String code, UUID laneId){
        return laneRepository.existsByGateIdAndCodeAndLaneIdNot(gateId, code, laneId);
    }

    @Override
    public Optional<UUID> findZoneIdByGateId(UUID gateId){
        return gateRepository.findZoneIdByGateId(gateId);
    }

    @Override
    public boolean hasOpenSessionsInZone(UUID zoneId){
        return  parkingSessionRepository.existsByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN);
    }

    @Override
    public boolean hasOtherActiveOutLaneInZone(UUID zoneId, UUID exCludedLaneId){
        return laneRepository.existsOtherLaneInZoneByStatusAndDirection(
                zoneId,
                exCludedLaneId,
                LaneStatus.ACTIVE,
                LaneDirection.OUT
        );
    }
}
