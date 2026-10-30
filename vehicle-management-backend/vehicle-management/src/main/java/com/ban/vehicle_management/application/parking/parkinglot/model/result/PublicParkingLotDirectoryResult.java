package com.ban.vehicle_management.application.parking.parkinglot.model.result;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicParkingLotDirectoryResult(
        UUID parkingLotId,
        String code,
        String name,
        String addressDisplay,
        BigDecimal latitude,
        BigDecimal longitude,
        ParkingLotStatus status,
        long totalCapacity,
        long visitorAvailable,
        boolean hasAvailability,
        Instant snapshotAt,
        List<VehicleTypeAvailability> vehicleTypes
) {
    public record VehicleTypeAvailability(
            UUID vehicleTypeId,
            String code,
            String name,
            long totalCapacity,
            long visitorAvailable,
            long subscriptionAvailable
    ) {
    }
}
