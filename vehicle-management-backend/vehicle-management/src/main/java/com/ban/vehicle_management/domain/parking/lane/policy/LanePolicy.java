package com.ban.vehicle_management.domain.parking.lane.policy;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.shared.enumeration.LaneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class LanePolicy {

    public void initialize(Lane lane) {
        requireLane(lane);
        lane.setCode(normalizeRequired(lane.getCode(), "code"));
        lane.setName(normalizeRequired(lane.getName(), "name"));
        requireField(lane.getParkingLotId(), "parkingLotId");
        requireField(lane.getDirection(), "direction");
        if (lane.getStatus() == null) {
            lane.setStatus(LaneStatus.ACTIVE);
        }
        validateState(lane);
    }

    public void activate(Lane lane) {
        requireLane(lane);
        lane.setStatus(LaneStatus.ACTIVE);
        validateState(lane);
    }

    public void markMaintenance(Lane lane) {
        requireLane(lane);
        lane.setStatus(LaneStatus.MAINTENANCE);
        validateState(lane);
    }

    public void close(Lane lane) {
        requireLane(lane);
        lane.setStatus(LaneStatus.CLOSED);
        validateState(lane);
    }

    public void validateState(Lane lane) {
        requireLane(lane);
        lane.setCode(normalizeRequired(lane.getCode(), "code"));
        lane.setName(normalizeRequired(lane.getName(), "name"));
        requireField(lane.getParkingLotId(), "parkingLotId");
        requireField(lane.getDirection(), "direction");
        requireField(lane.getStatus(), "status");
    }

    private void requireLane(Lane lane) {
        requireField(lane, "lane");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}

