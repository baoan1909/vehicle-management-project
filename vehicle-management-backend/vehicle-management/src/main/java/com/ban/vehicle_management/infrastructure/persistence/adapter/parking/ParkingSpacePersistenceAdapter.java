package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingSpacePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSpaceRepository;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ParkingSpacePersistenceAdapter implements ParkingSpacePortOut {

    private final ParkingSpaceRepository repository;
    private final ParkingSpacePersistenceMapper mapper;

    @Override
    @Transactional
    public ParkingSpace save(ParkingSpace parkingSpace) {
        var entity = mapper.toEntity(parkingSpace);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public List<ParkingSpace> saveAll(List<ParkingSpace> parkingSpaces) {
        var entities = parkingSpaces.stream().map(mapper::toEntity).toList();
        var saved = repository.saveAll(entities);
        return saved.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingSpace> findById(UUID parkingSpaceId) {
        return repository.findById(parkingSpaceId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> findByZoneId(UUID zoneId) {
        return repository.findByZoneId(zoneId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> findByZoneIdAndStatus(UUID zoneId, ParkingSpaceStatus status) {
        return repository.findByZoneIdAndStatus(zoneId, status).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> findByZoneIdAndLifecycleStatus(UUID zoneId, String lifecycleStatus) {
        return repository.findByZoneIdAndLifecycleStatus(zoneId, lifecycleStatus).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingSpace> findByZoneIdAndCode(UUID zoneId, String code) {
        return repository.findByZoneIdAndCode(zoneId, code).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByZoneIdAndCode(UUID zoneId, String code) {
        return repository.existsByZoneIdAndCode(zoneId, code);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByZoneIdAndCodeAndParkingSpaceIdNot(UUID zoneId, String code, UUID parkingSpaceId) {
        return repository.existsByZoneIdAndCodeAndParkingSpaceIdNot(zoneId, code, parkingSpaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByZoneIdAndStatus(UUID zoneId, ParkingSpaceStatus status) {
        return repository.countByZoneIdAndStatus(zoneId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByZoneIdAndLifecycleStatus(UUID zoneId, String lifecycleStatus) {
        return repository.countByZoneIdAndLifecycleStatus(zoneId, lifecycleStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> findOccupiedByRegistered(UUID zoneId) {
        return repository.findOccupiedByRegistered(zoneId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> findOccupiedByVisitor(UUID zoneId) {
        return repository.findOccupiedByVisitor(zoneId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> findReserved(UUID zoneId) {
        return repository.findReserved(zoneId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> findAvailable(UUID zoneId) {
        return repository.findAvailable(zoneId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpace> findMaintenance(UUID zoneId) {
        return repository.findMaintenance(zoneId).stream().map(mapper::toDomain).toList();
    }
}