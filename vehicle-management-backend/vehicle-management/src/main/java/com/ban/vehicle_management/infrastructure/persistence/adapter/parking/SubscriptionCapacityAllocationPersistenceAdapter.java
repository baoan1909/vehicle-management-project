package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.capacityallocation.port.out.SubscriptionCapacityAllocationPortOut;
import com.ban.vehicle_management.domain.parking.capacityallocation.model.SubscriptionCapacityAllocation;
import com.ban.vehicle_management.infrastructure.mapper.parking.SubscriptionCapacityAllocationPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.SubscriptionCapacityAllocationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionCapacityAllocationPersistenceAdapter implements SubscriptionCapacityAllocationPortOut {
    private final SubscriptionCapacityAllocationRepository repository;
    private final SubscriptionCapacityAllocationPersistenceMapper mapper;

    @Override
    public SubscriptionCapacityAllocation save(SubscriptionCapacityAllocation allocation) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(allocation)));
    }

    @Override
    public Optional<SubscriptionCapacityAllocation> findLiveBySubscriptionId(UUID subscriptionId) {
        return repository.findLiveBySubscriptionId(subscriptionId).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionCapacityAllocation> findEffectiveBySubscriptionId(UUID subscriptionId, LocalDate date) {
        return repository.findEffectiveBySubscriptionId(subscriptionId, date).map(mapper::toDomain);
    }

    @Override
    public List<SubscriptionCapacityAllocation> findEffectiveByZoneId(UUID zoneId, LocalDate date) {
        return repository.findEffectiveByZoneId(zoneId, date).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countEffectiveByZoneId(UUID zoneId, LocalDate date) {
        return repository.findEffectiveByZoneId(zoneId, date).size();
    }

    @Override
    public long maxConcurrentReservations(UUID zoneId, LocalDate from, LocalDate to, UUID excludedAllocationId) {
        return repository.maxConcurrentReservations(zoneId, from, to, excludedAllocationId);
    }

    @Override
    public long maxConcurrentReservations(UUID zoneId) {
        return repository.maxConcurrentReservations(zoneId);
    }

    @Override
    public List<SubscriptionCapacityAllocation> findExpiredHolds(Instant now) {
        return repository.findExpiredHolds(now).stream().map(mapper::toDomain).toList();
    }
}
