package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.RevenueAllocationEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevenueAllocationRepository extends JpaRepository<RevenueAllocationEntity, UUID> {

    Optional<RevenueAllocationEntity> findByPaymentId(UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RevenueAllocationEntity r where r.revenueAllocationId = :id")
    Optional<RevenueAllocationEntity> findByIdForUpdate(@Param("id") UUID id);

    List<RevenueAllocationEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @Modifying
    @Query(value = """
            UPDATE billing.revenue_allocations SET settled_at = now()
            WHERE revenue_allocation_id IN (
                SELECT revenue_allocation_id FROM billing.revenue_allocations
                WHERE status = 'POSTED' AND settled_at IS NULL AND available_at <= :now
                ORDER BY available_at ASC LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            ) RETURNING revenue_allocation_id
            """, nativeQuery = true)
    List<UUID> claimDueAllocations(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
