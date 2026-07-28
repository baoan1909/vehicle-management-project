package com.ban.vehicle_management.infrastructure.persistence.adapter.accesscontrol;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.infrastructure.mapper.accesscontrol.SubscriptionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.accesscontrol.SubscriptionSpecifications;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionPortOut {

    private static final List<SubscriptionStatus> OVERLAP_BLOCKING_STATUSES = List.of(
            SubscriptionStatus.PENDING,
            SubscriptionStatus.PENDING_PAYMENT,
            SubscriptionStatus.PENDING_CARD,
            SubscriptionStatus.ACTIVE
    );

    private static final List<SubscriptionStatus> CAPACITY_HOLDING_STATUSES = List.of(
            SubscriptionStatus.PENDING_PAYMENT,
            SubscriptionStatus.PENDING_CARD,
            SubscriptionStatus.ACTIVE
    );

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPersistenceMapper subscriptionPersistenceMapper;

    public SubscriptionPersistenceAdapter(
            SubscriptionRepository subscriptionRepository,
            SubscriptionPersistenceMapper subscriptionPersistenceMapper
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPersistenceMapper = subscriptionPersistenceMapper;
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionEntity savedEntity = subscriptionRepository.saveAndFlush(
                subscriptionPersistenceMapper.toEntity(subscription)
        );
        return subscriptionPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Subscription> findById(UUID subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .map(subscriptionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Subscription> findActiveByCardId(UUID cardId, LocalDate businessDate) {
        return subscriptionRepository
                .findFirstByCardIdAndStatusAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByEffectiveFromDesc(
                        cardId,
                        SubscriptionStatus.ACTIVE,
                        businessDate,
                        businessDate
                )
                .map(subscriptionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Subscription> findLatestActiveByCardId(UUID cardId) {
        return subscriptionRepository
                .findFirstByCardIdAndStatusOrderByEffectiveFromDesc(cardId, SubscriptionStatus.ACTIVE)
                .map(subscriptionPersistenceMapper::toDomain);
    }

    @Override
    public List<Subscription> findAll(
            UUID customerId,
            UUID customerVehicleId,
            UUID cardId,
            UUID ticketTypeId,
            SubscriptionStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String keyword
    ) {
        return subscriptionRepository.findAll(
                        SubscriptionSpecifications.withFilters(
                                customerId,
                                customerVehicleId,
                                cardId,
                                ticketTypeId,
                                status,
                                effectiveFrom,
                                effectiveTo,
                                keyword
                        )
                )
                .stream()
                .map(subscriptionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsOverlappingSubscription(
            UUID customerVehicleId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            UUID excludedSubscriptionId
    ) {
        return subscriptionRepository.existsOverlappingSubscription(
                customerVehicleId,
                effectiveFrom,
                effectiveTo,
                OVERLAP_BLOCKING_STATUSES,
                excludedSubscriptionId
        );
    }

    @Override
    public long countReservedOrActiveByVehicleTypeId(UUID vehicleTypeId) {
        return subscriptionRepository.countByVehicleTypeIdAndStatusIn(
                vehicleTypeId,
                CAPACITY_HOLDING_STATUSES
        );
    }

    @Override
    public Optional<Subscription> findActiveByLicensePlate(String licensePlate, LocalDate businessDate) {
        return subscriptionRepository.findActiveByLicensePlate(
                        licensePlate,
                        SubscriptionStatus.ACTIVE,
                        businessDate
                )
                .stream()
                .findFirst()
                .map(subscriptionPersistenceMapper::toDomain);
    }

    @Override
    public List<Subscription> findExpiredPendingPaymentsForUpdate(
            Instant approvedAtCutoff,
            LocalDate requestedEffectiveDateCutoff
    ) {
        return subscriptionRepository.findExpiredPendingPaymentsForUpdate(
                        SubscriptionStatus.PENDING_PAYMENT,
                        approvedAtCutoff,
                        requestedEffectiveDateCutoff
                )
                .stream()
                .map(subscriptionPersistenceMapper::toDomain)
                .toList();
    }
}
