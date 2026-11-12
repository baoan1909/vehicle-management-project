package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.dashboard.VehicleTypeCountProjection;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
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

    @Query(value = """
        select count(*) from parking.parking_sessions
        where status='OPEN' and parking_space_id is not null
        """, nativeQuery = true)
    long countOpenWithSpaceForMetrics();

    @Query(value = """
        select parking_session_id from parking.parking_sessions
        where customer_id=:customerId and status in ('OPEN','LOST_CARD') limit 1
        """, nativeQuery = true)
    Optional<UUID> findDeletionBlockingSessionId(@Param("customerId") UUID customerId);

    @Query(value = """
        select session.parking_session_id as "resourceId", lot.organization_id as "organizationId",
               lot.parking_lot_id as "parkingLotId"
          from parking.parking_sessions session
          join parking.parking_lots lot on lot.parking_lot_id = session.parking_lot_id
         where lot.organization_id in (:organizationIds) and session.status in ('OPEN', 'LOST_CARD')
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findOrganizationDeletionBlockers(
            @Param("organizationIds") Collection<UUID> organizationIds);

    @Query(value = """
            select count(*) from parking.parking_sessions
            where check_in_time >= :from and check_in_time < :to
            """, nativeQuery = true)
    long countDashboardCheckIns(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            select count(*) from parking.parking_sessions
            where status = 'CLOSED' and check_out_time >= :from and check_out_time < :to
            """, nativeQuery = true)
    long countDashboardCheckOuts(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            select count(*) from parking.parking_sessions
            where status <> 'CANCELLED' and check_in_time < :at
              and (check_out_time is null or check_out_time >= :at)
            """, nativeQuery = true)
    long countDashboardParkingAt(@Param("at") Instant at);

    @Query(value = """
            select session.vehicle_type_id as "vehicleTypeId",
                   type.name as "vehicleTypeName",
                   count(*) as "total"
            from parking.parking_sessions session
            left join catalog.vehicle_types type on type.vehicle_type_id = session.vehicle_type_id
            where session.check_in_time >= :from and session.check_in_time < :to
            group by session.vehicle_type_id, type.name
            order by "total" desc
            """, nativeQuery = true)
    List<VehicleTypeCountProjection> findDashboardVehicleTypeCounts(
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            select parkingSession
            from ParkingSessionEntity parkingSession
            left join parkingSession.customer customer
            left join customer.userProfile profile
            left join profile.account account
            where parkingSession.parkingSessionId = :parkingSessionId
              and (parkingSession.customerId is null or account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
            """)
    Optional<ParkingSessionEntity> findBusinessVisibleById(@Param("parkingSessionId") UUID parkingSessionId);

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

    long countByVehicleTypeIdAndStatusIn(UUID vehicleTypeId, Collection<ParkingSessionStatus> statuses);

    boolean existsByParkingSpaceIdAndStatus(UUID parkingSpaceId, ParkingSessionStatus status);

    Optional<ParkingSessionEntity> findFirstByParkingSpaceIdAndStatus(UUID parkingSpaceId, ParkingSessionStatus status);

    List<ParkingSessionEntity> findByParkingSpaceIdInAndStatus(
            List<UUID> parkingSpaceIds, ParkingSessionStatus status);

    List<ParkingSessionEntity> findByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);

    long countByZoneIdAndStatus(UUID zoneId, ParkingSessionStatus status);

    long countByParkingLotIdAndStatus(UUID parkingLotId, ParkingSessionStatus status);

    long countByParkingLotIdAndVehicleTypeIdAndStatusIn(
            UUID parkingLotId,
            UUID vehicleTypeId,
            Collection<ParkingSessionStatus> statuses
    );
    Optional<ParkingSessionEntity> findFirstByCustomerIdAndParkingLotIdAndStatusOrderByCheckOutTimeDesc(
            UUID customerId, UUID parkingLotId, ParkingSessionStatus status);
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


