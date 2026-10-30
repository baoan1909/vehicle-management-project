package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ParkingSessionRepository extends JpaRepository<ParkingSessionEntity, UUID>, JpaSpecificationExecutor<ParkingSessionEntity> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select parkingSession from ParkingSessionEntity parkingSession where parkingSession.parkingSessionId = :parkingSessionId")
    Optional<ParkingSessionEntity> findByIdForUpdate(@Param("parkingSessionId") UUID parkingSessionId);

    boolean existsByCardId(UUID cardId);

    boolean existsByCardIdAndStatusIn(UUID cardId, Collection<ParkingSessionStatus> statuses);

    boolean existsByCardIdAndStatus(UUID cardId, ParkingSessionStatus status);

    boolean existsBySubscriptionIdAndStatus(UUID subscriptionId, ParkingSessionStatus status);

    Optional<ParkingSessionEntity> findFirstByCardIdAndStatus(UUID cardId, ParkingSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from ParkingSessionEntity session where session.cardId = :cardId and session.status = 'OPEN'")
    Optional<ParkingSessionEntity> findOpenByCardIdForUpdate(@Param("cardId") UUID cardId);

    Optional<ParkingSessionEntity> findFirstBySubscriptionIdAndStatus(
            UUID subscriptionId,
            ParkingSessionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from ParkingSessionEntity session where session.subscriptionId = :subscriptionId and session.status = 'OPEN'")
    Optional<ParkingSessionEntity> findOpenBySubscriptionIdForUpdate(
            @Param("subscriptionId") UUID subscriptionId
    );

    boolean existsByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<ParkingSessionStatus> statuses);

    boolean existsByParkingSpaceIdAndStatus(UUID parkingSpaceId, ParkingSessionStatus status);

    Optional<ParkingSessionEntity> findFirstByParkingSpaceIdAndStatus(UUID parkingSpaceId, ParkingSessionStatus status);

    List<ParkingSessionEntity> findByParkingSpaceIdInAndStatus(
            List<UUID> parkingSpaceIds, ParkingSessionStatus status);

    List<ParkingSessionEntity> findByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);

    long countByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);

    @Query("select count(session) from ParkingSessionEntity session where session.zoneId = :zoneId and session.status = 'OPEN' and (session.subscriptionId is not null or session.customerVehicleId is not null)")
    long countOpenRegisteredByZoneId(@Param("zoneId") UUID zoneId);

    @Query("select count(session) from ParkingSessionEntity session where session.zoneId = :zoneId and session.status = 'OPEN' and session.subscriptionId is null and session.customerVehicleId is null")
    long countOpenVisitorByZoneId(@Param("zoneId") UUID zoneId);

    boolean existsByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);

    @Query("""
            SELECT parkingSession
            FROM ParkingSessionEntity parkingSession
            WHERE parkingSession.licensePlateInNormalized = :licensePlateIn
              AND parkingSession.status = :status
            """)
    List<ParkingSessionEntity> findByLicensePlateInAndStatus(
            @Param("licensePlateIn") String licensePlateIn,
            @Param("status") ParkingSessionStatus status
    );

    @Override
    @EntityGraph(attributePaths = {
            "card",
            "card.cardType",
            "vehicleType",
            "zone",
            "zone.parkingLot",
            "zone.parkingLevel",
            "parkingLot",
            "parkingSpace",
            "parkingEvents",
            "parkingEvents.lane"
    })
    List<ParkingSessionEntity> findAll(Specification<ParkingSessionEntity> specification, Sort sort);
}


