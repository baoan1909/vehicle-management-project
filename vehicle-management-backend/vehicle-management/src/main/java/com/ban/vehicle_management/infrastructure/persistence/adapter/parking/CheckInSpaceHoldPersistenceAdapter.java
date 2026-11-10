package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.checkinspacehold.port.out.CheckInSpaceHoldPortOut;
import com.ban.vehicle_management.domain.parking.checkinspacehold.model.CheckInSpaceHold;
import com.ban.vehicle_management.infrastructure.mapper.parking.CheckInSpaceHoldPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.CheckInSpaceHoldRepository;
import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CheckInSpaceHoldPersistenceAdapter implements CheckInSpaceHoldPortOut {
    private final CheckInSpaceHoldRepository repository;
    private final CheckInSpaceHoldPersistenceMapper mapper;

    @Override
    public void lockTransactionKey(String lockKey) {
        repository.lockTransactionKey(lockKey);
    }

    @Override
    public CheckInSpaceHold save(CheckInSpaceHold hold) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(hold)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckInSpaceHold> findById(UUID holdId) {
        return repository.findById(holdId).map(mapper::toDomain);
    }

    @Override
    public Optional<CheckInSpaceHold> findByIdForUpdate(UUID holdId) {
        return repository.findByIdForUpdate(holdId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckInSpaceHold> findByRequestId(UUID requestId) {
        return repository.findByRequestId(requestId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckInSpaceHold> findHeldByCardId(UUID cardId) {
        return repository.findFirstByCardIdAndStatus(cardId, CheckInSpaceHoldStatus.HELD)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CheckInSpaceHold> findHeldByPlateAndLot(UUID parkingLotId, String plate) {
        return repository.findFirstByParkingLotIdAndLicensePlateNormalizedAndStatus(
                parkingLotId, plate, CheckInSpaceHoldStatus.HELD).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsHeldByParkingSpaceId(UUID parkingSpaceId) {
        return repository.existsByParkingSpaceIdAndStatus(parkingSpaceId, CheckInSpaceHoldStatus.HELD);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findHeldSpaceIdsByZone(UUID zoneId, Instant now) {
        return repository.findHeldSpaceIdsByZone(zoneId, now);
    }

    @Override
    public List<CheckInSpaceHold> findDueForUpdate(Instant now, int limit) {
        return repository.findDueForUpdate(now, limit).stream().map(mapper::toDomain).toList();
    }
}
