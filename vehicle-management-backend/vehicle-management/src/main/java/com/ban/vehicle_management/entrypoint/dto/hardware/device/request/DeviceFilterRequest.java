package com.ban.vehicle_management.entrypoint.dto.hardware.device.request;

import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import java.util.UUID;

public record DeviceFilterRequest(
        UUID parkingLotId,
        UUID laneId,
        DeviceType deviceType,
        DeviceStatus status,
        String keyword
) {
}