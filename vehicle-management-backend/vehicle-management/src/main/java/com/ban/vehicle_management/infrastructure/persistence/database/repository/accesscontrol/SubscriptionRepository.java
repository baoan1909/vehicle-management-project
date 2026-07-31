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

    boolean existsByCardId(UUID cardId);

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

    Optional<SubscriptionEntity> findFirstByCardIdAndStatusOrderByEffectiveFromDesc(
            UUID cardId,
            SubscriptionStatus status
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
        select subscription
        from SubscriptionEntity subscription
        join subscription.customerVehicle customerVehicle
        where upper(customerVehicle.licensePlate) = upper(:licensePlate)
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
}
