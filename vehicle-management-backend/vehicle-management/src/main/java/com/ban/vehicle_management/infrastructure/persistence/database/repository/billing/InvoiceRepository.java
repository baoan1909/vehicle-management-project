package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID>, JpaSpecificationExecutor<InvoiceEntity> {
    boolean existsByParkingSessionIdAndStatusIn(UUID parkingSessionId, Collection<InvoiceStatus> statuses);

    boolean existsBySubscriptionIdAndStatusIn(UUID subcriptionId, Collection<InvoiceStatus> statuses);

    boolean existsByLostCardReportIdAndStatusIn(UUID lostCardReportId, Collection<InvoiceStatus> statuses);

    Optional<InvoiceEntity> findFirstBySubscriptionIdAndStatus(UUID subscriptionId, InvoiceStatus status);

    Optional<InvoiceEntity> findFirstBySubscriptionIdAndStatusIn(
            UUID subscriptionId,
            Collection<InvoiceStatus> statuses
    );
}


