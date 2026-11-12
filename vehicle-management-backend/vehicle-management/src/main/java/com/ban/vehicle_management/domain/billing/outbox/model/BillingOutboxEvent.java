package com.ban.vehicle_management.domain.billing.outbox.model;

import com.ban.vehicle_management.shared.enumeration.billing.BillingOutboxStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillingOutboxEvent {
    private UUID eventId;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private String payload;
    private BillingOutboxStatus status;
    private int retryCount;
    private Instant nextRetryAt;
    private Instant createdAt;
    private Instant processedAt;
    private String lastError;
    private String correlationId;
    private Instant updatedAt;
}
