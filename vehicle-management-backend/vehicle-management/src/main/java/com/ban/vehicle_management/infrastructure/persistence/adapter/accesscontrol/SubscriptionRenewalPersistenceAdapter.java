package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionRenewalPortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.SubscriptionRenewal;
import com.ban.vehicle_management.infrastructure.mapper.accesscontrol.SubscriptionRenewalPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRenewalRepository;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionRenewalStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRenewalPersistenceAdapter implements SubscriptionRenewalPortOut {
    private static final List<SubscriptionRenewalStatus> LIVE_STATUSES = List.of(
            SubscriptionRenewalStatus.PENDING_PAYMENT);
    private final SubscriptionRenewalRepository repository;
    private final SubscriptionRenewalPersistenceMapper mapper;

    public SubscriptionRenewalPersistenceAdapter(
            SubscriptionRenewalRepository repository,
            SubscriptionRenewalPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SubscriptionRenewal save(SubscriptionRenewal renewal) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(renewal)));
    }

    @Override
    public Optional<SubscriptionRenewal> findByInvoiceIdForUpdate(UUID invoiceId) {
        return repository.findByInvoiceIdForUpdate(invoiceId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionRenewal> findBySubscriptionIdAndIdempotencyKey(
            UUID subscriptionId, String idempotencyKey) {
        return repository.findBySubscriptionIdAndIdempotencyKey(subscriptionId, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionRenewal> findLiveBySubscriptionId(UUID subscriptionId) {
        return repository.findFirstBySubscriptionIdAndStatusInOrderByCreatedAtDesc(subscriptionId, LIVE_STATUSES)
                .map(mapper::toDomain);
    }

    @Override
    public List<SubscriptionRenewal> findScheduledStartingOnOrBefore(LocalDate businessDate) {
        return repository.findByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromAsc(
                        SubscriptionRenewalStatus.SCHEDULED, businessDate)
                .stream().map(mapper::toDomain).toList();
    }
}
