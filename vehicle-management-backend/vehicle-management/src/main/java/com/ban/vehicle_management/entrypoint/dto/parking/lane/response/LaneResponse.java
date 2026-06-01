package com.ban.vehicle_management.entrypoint.dto.parking.lane.response;

import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class LaneResponse {
    private UUID laneId;
    private UUID gateId;
    private String code;
    private String name;
    private LaneDirection direction;
    private LaneStatus status;
    private  String createdAt;
    private UUID createdBy;
    private  String updatedAt;
    private UUID updatedBy;
}
