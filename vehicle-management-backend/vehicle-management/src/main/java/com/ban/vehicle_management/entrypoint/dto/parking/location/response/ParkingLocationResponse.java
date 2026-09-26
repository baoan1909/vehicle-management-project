package com.ban.vehicle_management.entrypoint.dto.parking.location.response;

import java.math.BigDecimal;

public record ParkingLocationResponse(
        String displayName,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
