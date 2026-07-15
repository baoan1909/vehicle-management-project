package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingSessionManagementPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingSessionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.parking.ParkingSessionSpecifications;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ParkingSessionPersistenceAdapter implements ParkingSessionPortOut {

    private final ParkingSessionRepository parkingSessionRepository;
    private final ParkingSessionPersistenceMapper parkingSessionPersistenceMapper;
    private final ParkingSessionManagementPersistenceMapper parkingSessionManagementPersistenceMapper;

    public ParkingSessionPersistenceAdapter(
            ParkingSessionRepository parkingSessionRepository,
            ParkingSessionPersistenceMapper parkingSessionPersistenceMapper,
            ParkingSessionManagementPersistenceMapper parkingSessionManagementPersistenceMapper
    ) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.parkingSessionPersistenceMapper = parkingSessionPersistenceMapper;
        this.parkingSessionManagementPersistenceMapper = parkingSessionManagementPersistenceMapper;
    }

    @Override
    public ParkingSession save(ParkingSession parkingSession) {
        ParkingSessionEntity savedEntity = parkingSessionRepository.saveAndFlush(
                parkingSessionPersistenceMapper.toEntity(parkingSession)
        );
        return parkingSessionPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ParkingSession> findOpenByCardId(UUID cardId) {
        return parkingSessionRepository.findFirstByCardIdAndStatus(cardId, ParkingSessionStatus.OPEN)
                .map(parkingSessionPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsOpenByCardId(UUID cardId) {
        return parkingSessionRepository.existsByCardIdAndStatus(cardId, ParkingSessionStatus.OPEN);
    }

    @Override
    public long countOpenByZoneId(UUID zoneId) {
        return parkingSessionRepository.countByZoneIdAndStatus(zoneId, ParkingSessionStatus.OPEN);
    }

    @Override
    public List<ParkingSession> findOpenByLicensePlateIn(String licensePlateIn) {
        return parkingSessionRepository.findByLicensePlateInAndStatus(
                        licensePlateIn,
                        ParkingSessionStatus.OPEN
                )
                .stream()
                .map(parkingSessionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ParkingSession> findById(UUID parkingSessionId) {
        return parkingSessionRepository.findById(parkingSessionId)
                .map(parkingSessionPersistenceMapper::toDomain);
    }

    @Override
    public List<ParkingSessionManagementResult> findManagementSessions(
            ParkingSessionStatus status,
            UUID vehicleTypeId,
            UUID zoneId,
            Instant checkInFrom,
            Instant checkInTo,
            String keyword,
            List<UUID> customerVehicleIds
    ) {
        List<ParkingSessionEntity> entities = parkingSessionRepository.findAll(
                ParkingSessionSpecifications.withFilters(
                        status,
                        vehicleTypeId,
                        zoneId,
                        checkInFrom,
                        checkInTo,
                        keyword,
                        customerVehicleIds
                ),
                Sort.by(Sort.Direction.DESC, "checkInTime")
        );
        return parkingSessionManagementPersistenceMapper.toResults(entities);
    }
}
