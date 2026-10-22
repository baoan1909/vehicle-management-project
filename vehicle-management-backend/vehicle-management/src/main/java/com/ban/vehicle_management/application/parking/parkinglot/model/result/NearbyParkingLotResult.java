package com.ban.vehicle_management.application.parking.parkinglot.model.result;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record NearbyParkingLotResult(
        UUID parkingLotId,
        String name,
        String addressDisplay,
        BigDecimal latitude,
        BigDecimal longitude,
        ParkingLotStatus status,
        double distanceMeters
) {
}
