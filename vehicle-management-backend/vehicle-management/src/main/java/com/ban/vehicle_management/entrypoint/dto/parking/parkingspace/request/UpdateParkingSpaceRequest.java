package com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateParkingSpaceRequest(
        String code,
        UUID vehicleTypeId,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation
) {
}
