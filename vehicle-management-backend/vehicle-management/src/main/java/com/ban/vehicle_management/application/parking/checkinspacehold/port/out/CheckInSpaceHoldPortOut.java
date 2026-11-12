package com.ban.vehicle_management.application.parking.checkinspacehold.port.out;

import com.ban.vehicle_management.domain.parking.checkinspacehold.model.CheckInSpaceHold;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInSpaceHoldPortOut {
    void lockTransactionKey(String lockKey);
    CheckInSpaceHold save(CheckInSpaceHold hold);
    Optional<CheckInSpaceHold> findById(UUID holdId);
    Optional<CheckInSpaceHold> findByIdForUpdate(UUID holdId);
    Optional<CheckInSpaceHold> findByRequestId(UUID requestId);
    Optional<CheckInSpaceHold> findHeldByCardId(UUID cardId);
    Optional<CheckInSpaceHold> findHeldByPlateAndLot(UUID parkingLotId, String licensePlateNormalized);
    boolean existsHeldByParkingSpaceId(UUID parkingSpaceId);
    List<UUID> findHeldSpaceIdsByZone(UUID zoneId, Instant now);
    List<CheckInSpaceHold> findDueForUpdate(Instant now, int limit);
}
