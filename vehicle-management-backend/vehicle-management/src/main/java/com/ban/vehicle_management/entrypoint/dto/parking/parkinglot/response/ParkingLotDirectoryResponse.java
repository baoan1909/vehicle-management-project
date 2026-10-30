package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ParkingLotDirectoryResponse(
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
        List<VehicleTypeAvailabilityResponse> vehicleTypes
) {
    public record VehicleTypeAvailabilityResponse(
            UUID vehicleTypeId,
            String code,
            String name,
            long totalCapacity,
            long visitorAvailable,
            long subscriptionAvailable
    ) {
    }
}
