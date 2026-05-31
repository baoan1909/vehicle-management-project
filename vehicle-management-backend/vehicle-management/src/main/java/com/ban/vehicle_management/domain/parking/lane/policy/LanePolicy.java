package com.ban.vehicle_management.domain.parking.lane.policy;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class LanePolicy {

    public void initialize(Lane lane){
        requireLane(lane);
        requireField(lane.getGateId(), "gateId");
        requireField(lane.getDirection(),"direction");
        lane.setCode(TextValidationUtils.normalizeCode(lane.getCode(), "code", 50));
        lane.setName(TextValidationUtils.normalizeRequiredText(lane.getName(), "name", 150));

        if (lane.getStatus() == null)
        {
            lane.setStatus(LaneStatus.ACTIVE);
        }
        validateState(lane);
    }

    public  void activate(Lane lane){
        requireLane(lane);
        lane.setStatus(LaneStatus.ACTIVE);
        validateState(lane);
    }
    public void markMaintenance(Lane lane){
        requireLane(lane);
        lane.setStatus(LaneStatus.MAINTENANCE);
        validateState(lane);
    }
    public void close(Lane lane){
        requireLane(lane);
        lane.setStatus(LaneStatus.CLOSED);
        validateState(lane);
    }

    public void  validateState(Lane lane){
        requireLane(lane);
        requireField(lane.getGateId(), "gateId");
        requireField(lane.getDirection(), "direction");
        requireField(lane.getStatus(), "status");
        lane.setCode(TextValidationUtils.normalizeCode(lane.getCode(), "code", 50));
        lane.setName(TextValidationUtils.normalizeRequiredText(lane.getName(), "name", 150));
    }
    private void requireLane(Lane lane){
        requireField(lane, "lane");
    }
    private void requireField(Object value, String fieldName){
        if (value == null){
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

}

