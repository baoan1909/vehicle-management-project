package com.ban.vehicle_management.entrypoint.dto.hardware.device.request;

import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import java.util.Map;
import java.util.UUID;

public record CreateDeviceRequest(
        UUID parkingLotId,
        UUID laneId,
        String deviceCode,
        DeviceType deviceType,
        String name,
        String ipAddress,
        Map<String, Object> config
) {
}