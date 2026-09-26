package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingLotPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.GateRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.parking.ParkingLotSpecifications;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotPersistenceAdapter implements ParkingLotPortOut {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingLotPersistenceMapper parkingLotPersistenceMapper;
    private final ZoneRepository zoneRepository;
    private final GateRepository gateRepository;
    private final LaneRepository laneRepository;

    public ParkingLotPersistenceAdapter(
            ParkingLotRepository parkingLotRepository,
            ZoneRepository zoneRepository,
            GateRepository gateRepository,
            LaneRepository laneRepository,
            ParkingLotPersistenceMapper parkingLotPersistenceMapper
    ) {
        this.parkingLotRepository = parkingLotRepository;
        this.zoneRepository = zoneRepository;
        this.gateRepository = gateRepository;
        this.laneRepository = laneRepository;
        this.parkingLotPersistenceMapper = parkingLotPersistenceMapper;
    }

    @Override
    public ParkingLot save(ParkingLot parkingLot) {
        ParkingLotEntity savedEntity = parkingLotRepository.saveAndFlush(
                parkingLotPersistenceMapper.toEntity(parkingLot)
        );
        return parkingLotPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ParkingLot> findById(UUID parkingLotId) {
        return parkingLotRepository.findById(parkingLotId)
                .map(parkingLotPersistenceMapper::toDomain);
    }

    @Override
    public List<ParkingLot> findAll(
            ParkingLotStatus status,
            String keyword,
            Set<UUID> organizationIds,
            Set<UUID> parkingLotIds
    ) {
        return parkingLotRepository.findAll(
                        ParkingLotSpecifications.withFilters(status, keyword, organizationIds, parkingLotIds)
                )
                .stream()
                .map(parkingLotPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByOrganizationIdAndCode(UUID organizationId, String code) {
        return parkingLotRepository.existsByOrganizationIdAndCode(organizationId, code);
    }

    @Override
    public boolean existsByOrganizationIdAndCodeAndParkingLotIdNot(
            UUID organizationId,
            String code,
            UUID parkingLotId
    ) {
        return parkingLotRepository.existsByOrganizationIdAndCodeAndParkingLotIdNot(
                organizationId,
                code,
                parkingLotId
        );
    }

    @Override
    public boolean hasActiveZones(UUID parkingLotId) {
        return zoneRepository.existsByParkingLotIdAndStatus(parkingLotId, ZoneStatus.ACTIVE);
    }

    @Override
    public boolean isReadyForActivation(UUID parkingLotId) {
        return hasActiveZones(parkingLotId)
                && gateRepository.existsActiveGateByParkingLotId(parkingLotId)
                && laneRepository.existsActiveByParkingLotIdAndDirection(parkingLotId, LaneDirection.IN)
                && laneRepository.existsActiveByParkingLotIdAndDirection(parkingLotId, LaneDirection.OUT);
    }
}
