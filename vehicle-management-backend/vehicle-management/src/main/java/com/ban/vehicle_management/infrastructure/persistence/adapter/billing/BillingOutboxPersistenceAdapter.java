package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.outbox.port.out.BillingOutboxPortOut;
import com.ban.vehicle_management.domain.billing.outbox.model.BillingOutboxEvent;
import com.ban.vehicle_management.infrastructure.mapper.billing.BillingOutboxPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.BillingOutboxEventEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.BillingOutboxEventRepository;
import com.ban.vehicle_management.shared.enumeration.billing.BillingOutboxStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BillingOutboxPersistenceAdapter implements BillingOutboxPortOut {

    private final BillingOutboxEventRepository repository;
    private final BillingOutboxPersistenceMapper mapper;

    public BillingOutboxPersistenceAdapter(
            BillingOutboxEventRepository repository,
            BillingOutboxPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void enqueuePaymentCompleted(UUID paymentId, String correlationId, Instant createdAt) {
        String payload = "{\"paymentId\":\"" + paymentId + "\"}";
        repository.enqueuePaymentCompleted(
                UUID.randomUUID(), paymentId, payload, correlationId, createdAt);
    }

    @Override
    @Transactional
    public List<UUID> claimDue(int batchSize, Instant now, Instant staleBefore) {
        return repository.claimDue(batchSize, now, staleBefore);
    }

    @Override
    public Optional<BillingOutboxEvent> findById(UUID eventId) {
        return repository.findById(eventId).map(mapper::toDomain);
    }

    @Override
    public void markProcessed(UUID eventId, Instant processedAt) {
        BillingOutboxEventEntity event = requireEvent(eventId);
        event.setStatus(BillingOutboxStatus.PROCESSED);
        event.setProcessedAt(processedAt);
        event.setNextRetryAt(null);
        event.setLastError(null);
        event.setUpdatedAt(processedAt);
        repository.save(event);
    }

    @Override
    public boolean markFailed(UUID eventId, Instant nextRetryAt, String lastError, int maxRetries) {
        BillingOutboxEventEntity event = requireEvent(eventId);
        int retryCount = event.getRetryCount() + 1;
        boolean deadLetter = retryCount >= maxRetries;
        event.setRetryCount(retryCount);
        event.setStatus(deadLetter ? BillingOutboxStatus.DEAD_LETTER : BillingOutboxStatus.RETRY);
        event.setNextRetryAt(deadLetter ? null : nextRetryAt);
        event.setLastError(lastError);
        event.setUpdatedAt(Instant.now());
        repository.save(event);
        return deadLetter;
    }

    private BillingOutboxEventEntity requireEvent(UUID eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Billing outbox event not found"));
    }
}
