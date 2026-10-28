package com.ban.vehicle_management.application.parking.capacityallocation.port.out;

import com.ban.vehicle_management.domain.parking.capacityallocation.model.SubscriptionCapacityAllocation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionCapacityAllocationPortOut {
    SubscriptionCapacityAllocation save(SubscriptionCapacityAllocation allocation);
    Optional<SubscriptionCapacityAllocation> findLiveBySubscriptionId(UUID subscriptionId);
    Optional<SubscriptionCapacityAllocation> findEffectiveBySubscriptionId(UUID subscriptionId, LocalDate date);
    List<SubscriptionCapacityAllocation> findEffectiveByZoneId(UUID zoneId, LocalDate date);
    long countEffectiveByZoneId(UUID zoneId, LocalDate date);
    long maxConcurrentReservations(UUID zoneId, LocalDate effectiveFrom, LocalDate effectiveTo,
            UUID excludedAllocationId);
    long maxConcurrentReservations(UUID zoneId);
    List<SubscriptionCapacityAllocation> findExpiredHolds(Instant now);
}

