package com.ban.vehicle_management.application.parking.lane.port.in;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;

import java.util.List;
import java.util.UUID;

public interface LanePortIn {

    Lane createLane(Lane lane);

    Lane getLaneById(UUID laneId);

    List<Lane> getLanes(UUID gateId, LaneDirection direction, LaneStatus laneStatus, String keyword);

    Lane updateLane(UUID laneId, Lane lane);

    void deleteLane(UUID laneId);

    Lane activateLane(UUID laneId);

    Lane markLaneMaintenance(UUID laneId);

    Lane forceLaneMaintenance(UUID laneId);

    Lane closeLane(UUID laneId);
}
