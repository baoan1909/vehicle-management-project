package com.ban.vehicle_management.application.parking.location.model;

import java.math.BigDecimal;

public record ParkingLocationSearchResult(
        String displayName,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
