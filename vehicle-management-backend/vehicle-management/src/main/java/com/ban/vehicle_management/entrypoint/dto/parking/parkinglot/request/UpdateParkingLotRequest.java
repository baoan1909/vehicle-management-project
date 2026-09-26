package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

import java.math.BigDecimal;

public record UpdateParkingLotRequest(
        String code,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer totalCapacity
) {
}
