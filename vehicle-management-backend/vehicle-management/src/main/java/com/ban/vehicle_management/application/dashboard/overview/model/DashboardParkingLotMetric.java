package com.ban.vehicle_management.application.dashboard.overview.model;

import java.util.UUID;

public record DashboardParkingLotMetric(
        UUID parkingLotId,
        long currentParkingCount,
        long offlineDeviceCount
) {
}
