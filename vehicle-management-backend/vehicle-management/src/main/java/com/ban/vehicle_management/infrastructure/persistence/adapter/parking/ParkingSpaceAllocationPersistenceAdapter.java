package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingSpaceAllocationPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSpaceAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ParkingSpaceAllocationPersistenceAdapter implements ParkingSpaceAllocationPortOut {

    private final ParkingSpaceAllocationRepository repository;
    private final ParkingSpaceAllocationPersistenceMapper mapper;

    @Override
    @Transactional
    public ParkingSpaceAllocation save(ParkingSpaceAllocation allocation) {
        var entity = mapper.toEntity(allocation);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingSpaceAllocation> findById(UUID allocationId) {
        return repository.findById(allocationId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> findByParkingSpaceId(UUID parkingSpaceId) {
        return repository.findByParkingSpaceId(parkingSpaceId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> findBySubscriptionId(UUID subscriptionId) {
        return repository.findBySubscriptionId(subscriptionId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> findActiveAllocationsForSpaceOnDate(UUID parkingSpaceId, LocalDate date) {
        return repository.findActiveAllocationsForSpaceOnDate(parkingSpaceId, date).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> findActiveAllocationsBySubscription(UUID subscriptionId) {
        return repository.findActiveAllocationsBySubscription(subscriptionId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceAllocation> findExpiredHolds(UUID parkingSpaceId, Instant now) {
        return repository.findExpiredHolds(parkingSpaceId, now).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsOverlappingAllocation(UUID parkingSpaceId, LocalDate effectiveFrom, LocalDate effectiveTo, UUID excludeAllocationId) {
        return repository.existsOverlappingActiveAllocation(
                parkingSpaceId, effectiveFrom, effectiveTo, excludeAllocationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingSpaceAllocation> findByParkingSpaceIdAndSubscriptionIdAndEffectiveDates(UUID parkingSpaceId, UUID subscriptionId, LocalDate effectiveFrom, LocalDate effectiveTo) {
        return repository.findByParkingSpaceIdAndSubscriptionIdAndEffectiveFromAndEffectiveTo(parkingSpaceId, subscriptionId, effectiveFrom, effectiveTo).map(mapper::toDomain);
    }
}