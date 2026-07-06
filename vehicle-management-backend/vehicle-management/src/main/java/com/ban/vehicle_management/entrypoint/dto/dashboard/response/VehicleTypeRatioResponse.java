package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.util.List;

public record VehicleTypeRatioResponse(
        long total,
        List<VehicleTypeRatioItemResponse> items
) {
}