package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.BillingOutboxEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingOutboxEventRepository extends JpaRepository<BillingOutboxEventEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO billing.outbox_events (
                event_id, aggregate_type, aggregate_id, event_type, payload, status,
                retry_count, created_at, correlation_id, updated_at
            ) VALUES (
                :eventId, 'PAYMENT', :paymentId, 'PAYMENT_COMPLETED',
                CAST(:payload AS TEXT), 'PENDING', 0, :createdAt, :correlationId, :createdAt
            )
            ON CONFLICT (aggregate_type, aggregate_id, event_type) DO NOTHING
            """, nativeQuery = true)
    int enqueuePaymentCompleted(
            @Param("eventId") UUID eventId,
            @Param("paymentId") UUID paymentId,
            @Param("payload") String payload,
            @Param("correlationId") String correlationId,
            @Param("createdAt") Instant createdAt);

    @Modifying
    @Query(value = """
            UPDATE billing.outbox_events
            SET status = 'PROCESSING', updated_at = :now
            WHERE event_id IN (
                SELECT event_id
                FROM billing.outbox_events
                WHERE (
                    status = 'PENDING'
                    OR (status = 'RETRY' AND (next_retry_at IS NULL OR next_retry_at <= :now))
                    OR (status = 'PROCESSING' AND updated_at <= :staleBefore)
                )
                ORDER BY created_at ASC
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            )
            RETURNING event_id
            """, nativeQuery = true)
    List<UUID> claimDue(
            @Param("batchSize") int batchSize,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore);
}
