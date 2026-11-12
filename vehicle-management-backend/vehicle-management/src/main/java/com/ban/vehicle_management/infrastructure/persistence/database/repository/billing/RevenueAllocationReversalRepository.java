package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.RevenueAllocationReversalEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevenueAllocationReversalRepository
        extends JpaRepository<RevenueAllocationReversalEntity, UUID> {

    Optional<RevenueAllocationReversalEntity> findByRefundPaymentId(UUID refundPaymentId);

    List<RevenueAllocationReversalEntity> findByRevenueAllocationId(UUID revenueAllocationId);
}
