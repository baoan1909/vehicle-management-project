package com.ban.vehicle_management.entrypoint.dto.parking.lane.request;

import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;

import java.util.UUID;

public record CreateLaneRequest (
        UUID gateId,
        String code,
        String name,
        LaneDirection direction
){
}
