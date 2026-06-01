package com.ban.vehicle_management.entrypoint.dto.parking.lane.request;

import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;

public record UpdateLaneRequest (
        String code,
        String name,
        LaneDirection direction
){
}
