package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.shared.enumeration.billing.BillingOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_events", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillingOutboxEventEntity {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BillingOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
