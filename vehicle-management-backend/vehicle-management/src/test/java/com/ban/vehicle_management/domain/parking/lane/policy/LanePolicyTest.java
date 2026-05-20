package com.ban.vehicle_management.domain.parking.lane.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LanePolicyTest {

    private final LanePolicy lanePolicy = new LanePolicy();

    @Test
    void shouldInitializeLaneWithActiveStatus() {
        Lane lane = new Lane();
        lane.setParkingLotId(UUID.randomUUID());
        lane.setCode(" IN-01 ");
        lane.setName(" Entrance Lane ");
        lane.setDirection(LaneDirection.IN);

        lanePolicy.initialize(lane);

        assertEquals("IN-01", lane.getCode());
        assertEquals("Entrance Lane", lane.getName());
        assertEquals(LaneStatus.ACTIVE, lane.getStatus());
    }

    @Test
    void shouldRejectLaneWithoutDirection() {
        Lane lane = new Lane();
        lane.setParkingLotId(UUID.randomUUID());
        lane.setCode("IN-01");
        lane.setName("Entrance Lane");

        assertThrows(BadRequestException.class, () -> lanePolicy.initialize(lane));
    }

    @Test
    void shouldRejectLaneNameExceedingSchemaLength() {
        Lane lane = new Lane();
        lane.setParkingLotId(UUID.randomUUID());
        lane.setCode("IN-01");
        lane.setName("A".repeat(151));
        lane.setDirection(LaneDirection.IN);

        assertThrows(BadRequestException.class, () -> lanePolicy.initialize(lane));
    }
}

