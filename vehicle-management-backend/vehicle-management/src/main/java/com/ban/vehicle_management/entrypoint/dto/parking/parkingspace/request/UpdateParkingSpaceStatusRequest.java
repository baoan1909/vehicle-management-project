package com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request;

import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;

public record UpdateParkingSpaceStatusRequest(
        ParkingSpaceStatus targetStatus,
        ManualOccupancyType occupancyType,
        String reason
) {
}
