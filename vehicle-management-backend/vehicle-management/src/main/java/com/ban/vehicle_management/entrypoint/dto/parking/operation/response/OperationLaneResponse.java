package com.ban.vehicle_management.entrypoint.dto.parking.operation.response;

import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class OperationLaneResponse {
    private UUID laneId;
    private UUID gateId;
    private String code;
    private String name;
    private LaneDirection direction;
    private LaneStatus status;




}
