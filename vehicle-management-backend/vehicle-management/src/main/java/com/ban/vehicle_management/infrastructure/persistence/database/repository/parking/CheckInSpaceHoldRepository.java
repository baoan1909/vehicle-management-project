package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.CheckInSpaceHoldEntity;
import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckInSpaceHoldRepository extends JpaRepository<CheckInSpaceHoldEntity, UUID> {

    @Query(value = """
            select pg_advisory_xact_lock(hashtextextended(cast(:lockKey as text), 0)) is null
            """, nativeQuery = true)
    boolean lockTransactionKey(@Param("lockKey") String lockKey);

    Optional<CheckInSpaceHoldEntity> findByRequestId(UUID requestId);

    Optional<CheckInSpaceHoldEntity> findFirstByCardIdAndStatus(UUID cardId, CheckInSpaceHoldStatus status);

    Optional<CheckInSpaceHoldEntity> findFirstByParkingLotIdAndLicensePlateNormalizedAndStatus(
            UUID parkingLotId, String licensePlateNormalized, CheckInSpaceHoldStatus status);

    boolean existsByParkingSpaceIdAndStatus(UUID parkingSpaceId, CheckInSpaceHoldStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select hold from CheckInSpaceHoldEntity hold where hold.holdId = :holdId")
    Optional<CheckInSpaceHoldEntity> findByIdForUpdate(@Param("holdId") UUID holdId);

    @Query("""
            select hold.parkingSpaceId from CheckInSpaceHoldEntity hold
            where hold.zoneId = :zoneId and hold.status = 'HELD' and hold.expiresAt > :now
            """)
    List<UUID> findHeldSpaceIdsByZone(@Param("zoneId") UUID zoneId, @Param("now") Instant now);

    @Query(value = """
            select hold.*
            from parking.check_in_space_holds hold
            where hold.status = 'HELD' and hold.expires_at <= :now
            order by hold.expires_at, hold.hold_id
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<CheckInSpaceHoldEntity> findDueForUpdate(
            @Param("now") Instant now,
            @Param("limit") int limit);
}
