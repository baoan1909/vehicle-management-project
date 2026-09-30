package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

import java.math.BigDecimal;

public record ConfirmParkingLotLocationRequest(
        BigDecimal latitude,
        BigDecimal longitude
) {
}
