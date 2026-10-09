package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingLevelPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLevelRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ParkingLevelPersistenceAdapter implements ParkingLevelPortOut {

    private final ParkingLevelRepository repository;
    private final ParkingLotRepository parkingLotRepository;
    private final ZoneRepository zoneRepository;
    private final ParkingLevelPersistenceMapper mapper;

    @Override
    @Transactional
    public ParkingLevel save(ParkingLevel parkingLevel) {
        var entity = mapper.toEntity(parkingLevel);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingLevel> findById(UUID parkingLevelId) {
        return repository.findById(parkingLevelId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLevel> findAll(UUID parkingLotId, ParkingLevelStatus status, String keyword) {
        List<com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLevelEntity> levels;
        if (status != null) {
            levels = repository.findByParkingLotIdAndStatusOrderByDisplayOrder(parkingLotId, status);
        } else {
            levels = repository.findByParkingLotIdOrderByDisplayOrder(parkingLotId);
        }
        if (keyword == null || keyword.isBlank()) {
            return levels.stream().map(mapper::toDomain).toList();
        }
        String normalized = keyword.trim().toUpperCase();
        return levels.stream()
                .filter(pl -> pl.getCode().toUpperCase().contains(normalized)
                        || pl.getName().toUpperCase().contains(normalized))
                .map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByParkingLotIdAndCode(UUID parkingLotId, String code) {
        return repository.existsByParkingLotIdAndCode(parkingLotId, code);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByParkingLotIdAndCodeAndParkingLevelIdNot(UUID parkingLotId, String code, UUID parkingLevelId) {
        return repository.findByParkingLotIdAndCode(parkingLotId, code)
                .map(existing -> !existing.getParkingLevelId().equals(parkingLevelId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsConfigurableParkingLotById(UUID parkingLotId) {
        return parkingLotRepository.existsByParkingLotIdAndStatusIn(
                parkingLotId,
                Set.of(ParkingLotStatus.SETUP, ParkingLotStatus.ACTIVE)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countByParkingLotId(UUID parkingLotId) {
        return repository.countByParkingLotId(parkingLotId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countZonesByParkingLevelId(UUID parkingLevelId) {
        return zoneRepository.findByParkingLevelId(parkingLevelId).size();
    }

    @Override
    @Transactional
    public void delete(UUID parkingLevelId) {
        repository.deleteById(parkingLevelId);
    }
}
