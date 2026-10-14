package com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request;

import java.math.BigDecimal;

public record UpdateParkingLevelRequest(
        String code,
        String name,
        Integer displayOrder,
        BigDecimal elevation,
        BigDecimal floorHeight,
        BigDecimal canvasWidth,
        BigDecimal canvasHeight
) {
}
