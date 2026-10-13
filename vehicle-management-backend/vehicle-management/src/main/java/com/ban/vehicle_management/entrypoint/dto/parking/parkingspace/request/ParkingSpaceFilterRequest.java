package com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import java.util.UUID;

public record ParkingSpaceFilterRequest(
        UUID zoneId,
        ParkingSpaceStatus status,
        String lifecycleStatus
) {
}
