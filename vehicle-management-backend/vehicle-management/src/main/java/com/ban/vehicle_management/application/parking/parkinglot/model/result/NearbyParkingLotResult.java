package com.ban.vehicle_management.application.parking.parkinglot.model.result;

import java.math.BigDecimal;
import java.util.UUID;

public record NearbyParkingLotResult(
        UUID parkingLotId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        double distanceMeters
) {
}
