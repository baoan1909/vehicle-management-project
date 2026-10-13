package com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateParkingLevelRequest(
        UUID parkingLotId,
        String code,
        String name,
        Integer displayOrder,
        BigDecimal elevation,
        BigDecimal floorHeight,
        BigDecimal canvasWidth,
        BigDecimal canvasHeight
) {
}
