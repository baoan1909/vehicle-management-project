package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ParkingLotPublicResponse(
        UUID parkingLotId,
        String name,
        String addressDisplay,
        BigDecimal latitude,
        BigDecimal longitude,
        ParkingLotStatus status,
        Long distanceMeters,
        BigDecimal distanceKm
) {
}
