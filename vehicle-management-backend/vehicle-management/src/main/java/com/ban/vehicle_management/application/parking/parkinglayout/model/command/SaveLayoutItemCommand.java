package com.ban.vehicle_management.application.parking.parkinglayout.model.command;

import java.math.BigDecimal;
import java.util.UUID;

public record SaveLayoutItemCommand(
        String code,
        UUID vehicleTypeId,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation
) {
}
