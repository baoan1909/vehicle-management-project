package com.ban.vehicle_management.entrypoint.dto.parking.lane.request;

import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;

import java.util.UUID;

public record LaneFilterRequest (
        UUID gateId,
        LaneDirection direction,
        LaneStatus status,
        String keyword
){
}
