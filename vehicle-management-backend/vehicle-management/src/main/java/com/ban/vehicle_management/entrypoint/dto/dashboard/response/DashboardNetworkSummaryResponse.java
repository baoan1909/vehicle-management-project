package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

public record DashboardNetworkSummaryResponse(
        long organizationCount,
        long parkingLotCount,
        long activeParkingLotCount,
        long totalCapacity,
        long offlineDeviceCount
) {
}
