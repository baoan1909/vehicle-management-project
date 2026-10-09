package com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out;

import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSpaceAllocationPortOut {

    ParkingSpaceAllocation save(ParkingSpaceAllocation allocation);

    Optional<ParkingSpaceAllocation> findById(UUID allocationId);

    List<ParkingSpaceAllocation> findByParkingSpaceId(UUID parkingSpaceId);

    List<ParkingSpaceAllocation> findBySubscriptionId(UUID subscriptionId);

    List<ParkingSpaceAllocation> findActiveAllocationsForSpaceOnDate(UUID parkingSpaceId, LocalDate date);

    List<ParkingSpaceAllocation> findActiveAllocationsBySubscription(UUID subscriptionId);

    List<ParkingSpaceAllocation> findExpiredHolds(UUID parkingSpaceId, Instant now);

    boolean existsOverlappingAllocation(UUID parkingSpaceId, LocalDate effectiveFrom, LocalDate effectiveTo, UUID excludeAllocationId);

    Optional<ParkingSpaceAllocation> findByParkingSpaceIdAndSubscriptionIdAndEffectiveDates(UUID parkingSpaceId, UUID subscriptionId, LocalDate effectiveFrom, LocalDate effectiveTo);
}