package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.infrastructure.mapper.parking.ZonePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.GateRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.parking.ZoneSpecifications;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ZonePersistenceAdapter implements ZonePortOut {

    private final ZoneRepository zoneRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final ZonePersistenceMapper zonePersistenceMapper;
    private final GateRepository gateRepository;

    public ZonePersistenceAdapter(
            ZoneRepository zoneRepository,
            ParkingLotRepository parkingLotRepository,
            VehicleTypeRepository vehicleTypeRepository,
            ParkingSessionRepository parkingSessionRepository,
            GateRepository gateRepository,
            ZonePersistenceMapper zonePersistenceMapper
    ) {
        this.zoneRepository = zoneRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.gateRepository = gateRepository;
        this.zonePersistenceMapper = zonePersistenceMapper;
    }

    @Override
    public Zone save(Zone zone) {
        ZoneEntity savedEntity = zoneRepository.saveAndFlush(zonePersistenceMapper.toEntity(zone));
        return zonePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Zone> findById(UUID zoneId) {
        return zoneRepository.findById(zoneId)
                .map(zonePersistenceMapper::toDomain);
    }

    @Override
    public List<Zone> findAll(UUID parkingLotId, UUID vehicleTypeId, ZoneStatus status, String keyword) {
        return zoneRepository.findAll(
                        ZoneSpecifications.withFilters(parkingLotId, vehicleTypeId, status, keyword)
                )
                .stream()
                .map(zonePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code) {
        return zoneRepository.existsByParkingLotIdAndCode(parkingLotId, code);
    }

    @Override
    public boolean existsByParkingLotIdAndCodeAndZoneIdNot(UUID parkingLotId, String code, UUID zoneId) {
        return zoneRepository.existsByParkingLotIdAndCodeAndZoneIdNot(parkingLotId, code, zoneId);
    }

    @Override
    public boolean existsActiveParkingLotById(UUID parkingLotId) {
        return parkingLotRepository.existsByParkingLotIdAndStatus(parkingLotId, ParkingLotStatus.ACTIVE);
    }

    @Override
    public boolean existsActiveVehicleTypeById(UUID vehicleTypeId) {
        if (vehicleTypeId == null) {
            return true;
        }
        return vehicleTypeRepository.existsByVehicleTypeIdAndIsActiveTrue(vehicleTypeId);
    }

    @Override
    public long countOpenSessions(UUID zoneId) {
        return parkingSessionRepository.countByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN);
    }

    @Override
    public boolean hasOpenSessions(UUID zoneId) {
        return parkingSessionRepository.existsByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN);
    }

    @Override
    public boolean hasActiveGates(UUID zoneId) {
        return gateRepository.existsByZoneIdAndStatus(zoneId, GateStatus.ACTIVE);
    }

    @Override
    public long sumActiveCapacityByVehicleTypeId(UUID vehicleTypeId) {
        return zoneRepository.sumActiveCapacityByVehicleTypeId(vehicleTypeId);
    }
}