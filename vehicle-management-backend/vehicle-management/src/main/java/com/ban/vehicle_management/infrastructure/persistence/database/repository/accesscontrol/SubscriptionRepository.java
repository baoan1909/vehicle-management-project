package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID>, JpaSpecificationExecutor<SubscriptionEntity> {

    @Query(value = """
        select subscription_id from access_control.subscriptions
        where customer_id=:customerId
          and status in ('PENDING','PENDING_PAYMENT','PENDING_CARD','ACTIVE') limit 1
        """, nativeQuery = true)
    Optional<UUID> findDeletionBlockingSubscriptionId(@Param("customerId") UUID customerId);

    @Query("""
            select subscription
            from SubscriptionEntity subscription
            join subscription.customer customer
            join customer.userProfile profile
            join profile.account account
            where subscription.subscriptionId = :subscriptionId
              and customer.status <> com.ban.vehicle_management.shared.enumeration.people.CustomerStatus.DELETED
              and profile.status <> com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus.DELETED
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
            """)
    Optional<SubscriptionEntity> findBusinessVisibleById(@Param("subscriptionId") UUID subscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subscription from SubscriptionEntity subscription where subscription.subscriptionId = :subscriptionId")
    Optional<SubscriptionEntity> findByIdForUpdate(@Param("subscriptionId") UUID subscriptionId);

    boolean existsByCardId(UUID cardId);

    Optional<SubscriptionEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    Optional<SubscriptionEntity> findByQrAccessToken(String qrAccessToken);

    @Query("""
            select priceRule.vehicleTypeId
            from SubscriptionEntity subscription
            join subscription.priceRule priceRule
            where subscription.subscriptionId = :subscriptionId
            """)
    Optional<UUID> findOperationalVehicleTypeIdBySubscriptionId(
            @Param("subscriptionId") UUID subscriptionId
    );

    boolean existsByCardIdAndStatusIn(UUID cardId, Collection<SubscriptionStatus> statuses);

    boolean existsByPriceRuleId(UUID priceRuleId);

    boolean existsByTicketTypeIdAndStatusIn(UUID ticketTypeId, Collection<SubscriptionStatus> statuses);

    Optional<SubscriptionEntity>
    findFirstByCardIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
            UUID cardId,
            SubscriptionStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select subscription from SubscriptionEntity subscription
        where subscription.cardId = :cardId
          and subscription.status = com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus.ACTIVE
          and subscription.effectiveFrom <= :businessDate
          and subscription.effectiveTo >= :businessDate
        order by subscription.effectiveFrom desc
        """)
    List<SubscriptionEntity> findActiveByCardIdForUpdate(
            @Param("cardId") UUID cardId, @Param("businessDate") LocalDate businessDate);

    Optional<SubscriptionEntity> findFirstByCardIdAndStatusOrderByEffectiveFromDesc(
            UUID cardId,
            SubscriptionStatus status
    );

    Optional<SubscriptionEntity> findFirstByCardIdAndStatusInOrderByApprovedAtDesc(
            UUID cardId,
            Collection<SubscriptionStatus> statuses
    );

    @Query("""
            select count(subscription) > 0
            from SubscriptionEntity subscription
            where subscription.customerVehicleId = :customerVehicleId
              and subscription.status in :statuses
              and (:excludedSubscriptionId is null or subscription.subscriptionId <> :excludedSubscriptionId)
              and subscription.effectiveFrom <= :effectiveTo
              and subscription.effectiveTo >= :effectiveFrom
            """)
    boolean existsOverlappingSubscription(
            @Param("customerVehicleId") UUID customerVehicleId,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo,
            @Param("statuses") Collection<SubscriptionStatus> statuses,
            @Param("excludedSubscriptionId") UUID excludedSubscriptionId
    );

    @Query("""
            select count(subscription) > 0 from SubscriptionEntity subscription
            where subscription.customerVehicleId = :customerVehicleId
              and subscription.parkingLotId = :parkingLotId
              and subscription.status in :statuses
              and (:excludedSubscriptionId is null or subscription.subscriptionId <> :excludedSubscriptionId)
              and subscription.effectiveFrom <= :effectiveTo
              and subscription.effectiveTo >= :effectiveFrom
            """)
    boolean existsOverlappingSubscriptionInParkingLot(UUID customerVehicleId, UUID parkingLotId,
            LocalDate effectiveFrom, LocalDate effectiveTo,
            Collection<SubscriptionStatus> statuses, UUID excludedSubscriptionId);

    @Query("""
            select count(subscription)
            from SubscriptionEntity subscription
            join subscription.customerVehicle customerVehicle
            where customerVehicle.vehicleTypeId = :vehicleTypeId
              and subscription.status in :statuses
            """)
    long countByVehicleTypeIdAndStatusIn(
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("statuses") Collection<SubscriptionStatus> statuses
    );

    @Query("""
            select count(subscription) from SubscriptionEntity subscription
            join subscription.customerVehicle customerVehicle
            where subscription.parkingLotId = :parkingLotId
              and customerVehicle.vehicleTypeId = :canonicalVehicleTypeId
              and subscription.status in :statuses
            """)
    long countByCanonicalVehicleTypeIdAndParkingLotIdAndStatusIn(
            UUID canonicalVehicleTypeId, UUID parkingLotId, Collection<SubscriptionStatus> statuses);

    @Query("""
            select count(subscription) from SubscriptionEntity subscription
            join subscription.customerVehicle customerVehicle
            where subscription.parkingLotId = :parkingLotId
              and customerVehicle.vehicleTypeId = :vehicleTypeId
              and subscription.status in :statuses
              and subscription.effectiveTo >= :businessDate
            """)
    long countServiceObligations(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("businessDate") LocalDate businessDate,
            @Param("statuses") Collection<SubscriptionStatus> statuses
    );

    @Query("""
            select count(subscription) from SubscriptionEntity subscription
            join subscription.customerVehicle customerVehicle
            where customerVehicle.vehicleTypeId = :vehicleTypeId
              and subscription.status in :statuses
              and subscription.effectiveTo >= :businessDate
            """)
    long countServiceObligations(
            @Param("vehicleTypeId") UUID vehicleTypeId,
            @Param("businessDate") LocalDate businessDate,
            @Param("statuses") Collection<SubscriptionStatus> statuses
    );

    @Query("""
        select subscription
        from SubscriptionEntity subscription
        join subscription.customerVehicle customerVehicle
        where customerVehicle.licensePlateNormalized = :licensePlate
          and subscription.status = :status
          and subscription.effectiveFrom <= :businessDate
          and subscription.effectiveTo >= :businessDate
        order by subscription.effectiveFrom desc
        """)
    List<SubscriptionEntity> findActiveByLicensePlate(
            @Param("licensePlate") String licensePlate,
            @Param("status") SubscriptionStatus status,
            @Param("businessDate") LocalDate businessDate
    );

    @Query("""
        select subscription
        from SubscriptionEntity subscription
        join subscription.customerVehicle customerVehicle
        where upper(customerVehicle.licensePlate) = upper(:licensePlate)
          and subscription.status = :status
          and subscription.effectiveFrom <= :businessDate
          and subscription.effectiveTo >= :businessDate
          and subscription.parkingLotId in :parkingLotIds
        order by subscription.effectiveFrom desc
        """)
    List<SubscriptionEntity> findActiveByLicensePlateInParkingLots(
            @Param("licensePlate") String licensePlate,
            @Param("status") SubscriptionStatus status,
            @Param("businessDate") LocalDate businessDate,
            @Param("parkingLotIds") java.util.Set<UUID> parkingLotIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select subscription
            from SubscriptionEntity subscription
            where subscription.status = :status
              and (
                    subscription.approvedAt <= :approvedAtCutoff
                    or subscription.requestedEffectiveFrom <= :requestedEffectiveDateCutoff
              )
            order by subscription.approvedAt asc
            """)
    List<SubscriptionEntity> findExpiredPendingPaymentsForUpdate(
            @Param("status") SubscriptionStatus status,
            @Param("approvedAtCutoff") Instant approvedAtCutoff,
            @Param("requestedEffectiveDateCutoff") LocalDate requestedEffectiveDateCutoff
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select subscription
            from SubscriptionEntity subscription
            where subscription.spaceSelectionRequired = true
              and subscription.spaceSelectionConfirmedAt is null
              and subscription.spaceSelectionExpiresAt is not null
              and subscription.spaceSelectionExpiresAt <= :now
              and subscription.status in :statuses
            order by subscription.spaceSelectionExpiresAt asc
            """)
    List<SubscriptionEntity> findExpiredSpaceSelectionsForUpdate(
            @Param("now") Instant now,
            @Param("statuses") Collection<SubscriptionStatus> statuses
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SubscriptionEntity subscription
            set subscription.status = :expiredStatus
            where subscription.status = :activeStatus
              and subscription.effectiveTo < :businessDate
            """)
    int expireActiveSubscriptionsBefore(
            @Param("businessDate") LocalDate businessDate,
            @Param("activeStatus") SubscriptionStatus activeStatus,
            @Param("expiredStatus") SubscriptionStatus expiredStatus
    );

    List<SubscriptionEntity> findByStatusAndEffectiveTo(
            SubscriptionStatus status,
            LocalDate effectiveTo
    );

    List<SubscriptionEntity> findByStatusAndEffectiveToBefore(
            SubscriptionStatus status,
            LocalDate businessDate
    );
}
