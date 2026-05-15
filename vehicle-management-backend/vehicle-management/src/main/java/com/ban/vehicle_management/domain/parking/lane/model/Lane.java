package com.ban.vehicle_management.domain.parking.lane.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.LaneStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lane extends AuditableDomainModel {

    private UUID laneId;
    private UUID parkingLotId;
    private String code;
    private String name;
    private LaneDirection direction;
    private LaneStatus status;
}

