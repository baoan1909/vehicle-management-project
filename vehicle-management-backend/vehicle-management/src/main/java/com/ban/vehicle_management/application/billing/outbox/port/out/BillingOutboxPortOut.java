package com.ban.vehicle_management.application.billing.outbox.port.out;

import com.ban.vehicle_management.domain.billing.outbox.model.BillingOutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingOutboxPortOut {

    void enqueuePaymentCompleted(UUID paymentId, String correlationId, Instant createdAt);

    List<UUID> claimDue(int batchSize, Instant now, Instant staleBefore);

    Optional<BillingOutboxEvent> findById(UUID eventId);

    void markProcessed(UUID eventId, Instant processedAt);

    boolean markFailed(UUID eventId, Instant nextRetryAt, String lastError, int maxRetries);
}
