package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ParkingLotPublicResponse(
        UUID parkingLotId,
        String name,
        String addressDisplay,
        BigDecimal latitude,
        BigDecimal longitude,
        Long distanceMeters,
        BigDecimal distanceKm
) {
}
