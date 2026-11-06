package com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionRenewalEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionRenewalStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRenewalRepository extends JpaRepository<SubscriptionRenewalEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SubscriptionRenewalEntity r where r.invoiceId = :invoiceId")
    Optional<SubscriptionRenewalEntity> findByInvoiceIdForUpdate(@Param("invoiceId") UUID invoiceId);

    Optional<SubscriptionRenewalEntity> findBySubscriptionIdAndIdempotencyKey(UUID subscriptionId, String idempotencyKey);

    Optional<SubscriptionRenewalEntity> findFirstBySubscriptionIdAndStatusInOrderByCreatedAtDesc(
            UUID subscriptionId, Collection<SubscriptionRenewalStatus> statuses);

    List<SubscriptionRenewalEntity> findByStatusAndEffectiveFromLessThanEqual(
            SubscriptionRenewalStatus status, LocalDate effectiveFrom);
}
