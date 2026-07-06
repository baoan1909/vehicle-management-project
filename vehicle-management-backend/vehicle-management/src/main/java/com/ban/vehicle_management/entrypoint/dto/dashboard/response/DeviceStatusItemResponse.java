package com.ban.vehicle_management.entrypoint.dto.dashboard.response;

public record DeviceStatusItemResponse(
        String deviceType,
        String deviceTypeName,
        long activeCount,
        long offlineCount,
        long maintenanceCount
) {
}