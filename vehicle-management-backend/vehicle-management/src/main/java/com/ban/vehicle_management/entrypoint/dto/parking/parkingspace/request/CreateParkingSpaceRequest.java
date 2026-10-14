package com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateParkingSpaceRequest(
        UUID zoneId,
        String code,
        UUID vehicleTypeId,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation
) {
}
