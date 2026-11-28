package com.ban.vehicle_management.entrypoint.dto.hardware.device.response;

import java.util.UUID;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;

public record DeviceLaneOptionResponse(UUID laneId, UUID parkingLotId, UUID gateId, String code, String name,
        LaneDirection direction, LaneStatus status, String gateName, String zoneName) {}
