package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleTypeRatioItemResponse(
        UUID vehicleTypeId,
        String vehicleTypeName,
        long count,
        BigDecimal percentage
) {
}