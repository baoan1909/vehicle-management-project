package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

import java.math.BigDecimal;
import java.util.UUID;

public record DashboardParkingLotSummaryResponse(
        UUID parkingLotId,
        UUID organizationId,
        String name,
        String status,
        long capacity,
        long currentParkingCount,
        BigDecimal occupancyRate,
        long offlineDeviceCount
) {
}
