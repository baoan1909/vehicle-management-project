package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingSessionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ParkingSessionPersistenceAdapter implements ParkingSessionPortOut {

    private final ParkingSessionRepository parkingSessionRepository;
    private final ParkingSessionPersistenceMapper parkingSessionPersistenceMapper;

    public ParkingSessionPersistenceAdapter(
            ParkingSessionRepository parkingSessionRepository,
            ParkingSessionPersistenceMapper parkingSessionPersistenceMapper
    ) {
        this.parkingSessionRepository = parkingSessionRepository;
        this.parkingSessionPersistenceMapper = parkingSessionPersistenceMapper;
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
}
