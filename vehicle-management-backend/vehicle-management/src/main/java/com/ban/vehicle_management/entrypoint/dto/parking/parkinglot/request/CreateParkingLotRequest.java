package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateParkingLotRequest(
        UUID organizationId,
        String code,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer totalCapacity
) {
}
