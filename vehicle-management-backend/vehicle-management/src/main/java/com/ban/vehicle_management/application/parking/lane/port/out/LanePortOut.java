package com.ban.vehicle_management.application.parking.lane.port.out;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LanePortOut {

    Lane save(Lane lane);

    Optional<Lane> findById(UUID laneId);

    List<Lane> findAll(UUID gateId, LaneDirection laneDirection, LaneStatus laneStatus, String keyword);

    boolean existsOperationalGateById(UUID gateId);

    boolean existsByGateIdAndCode(UUID gateId, String code);

    boolean existsByGateIdAndCodeAndLaneIdNot(UUID gateId, String code, UUID laneId);

    Optional<UUID> findZoneIdByGateId(UUID gateId);

    boolean hasOpenSessionsInZone(UUID zoneId);

    boolean hasOtherActiveOutLaneInZone(UUID zoneId, UUID excludedLaneId);
}
