package com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLevelStatus;
import java.util.UUID;

public record ParkingLevelFilterRequest(
        UUID parkingLotId,
        ParkingLevelStatus status,
        String keyword
) {
}
