package com.ban.vehicle_management.application.parking.parkingspaceallocation.port.in;

import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ParkingSpaceAllocationPortIn {

    ParkingSpaceAllocation createAllocation(ParkingSpaceAllocation allocation);

    ParkingSpaceAllocation getAllocationById(UUID allocationId);

    List<ParkingSpaceAllocation> getAllocationsByParkingSpaceId(UUID parkingSpaceId);

    List<ParkingSpaceAllocation> getAllocationsBySubscriptionId(UUID subscriptionId);

    ParkingSpaceAllocation activateAllocation(UUID allocationId);

    ParkingSpaceAllocation releaseAllocation(UUID allocationId);

    ParkingSpaceAllocation cancelAllocation(UUID allocationId);

    ParkingSpaceAllocation setHoldExpiration(UUID allocationId, java.time.Instant holdExpiresAt);

    List<ParkingSpaceAllocation> findActiveAllocationsForSpaceOnDate(UUID parkingSpaceId, LocalDate date);

    List<ParkingSpaceAllocation> findActiveAllocationsBySubscription(UUID subscriptionId);
}