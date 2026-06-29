package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingCheckOutPolicyTest {

    private final ParkingCheckOutPolicy parkingCheckOutPolicy = new ParkingCheckOutPolicy();

    @Test
    void shouldAcceptOperationalOutTopology() {
        assertDoesNotThrow(() -> parkingCheckOutPolicy.validateOperationalTopology(
                lane(LaneDirection.OUT, LaneStatus.ACTIVE),
                gate(GateStatus.ACTIVE),
                zone(ZoneStatus.ACTIVE),
                parkingLot(ParkingLotStatus.ACTIVE)
        ));
    }

    @Test
    void shouldRejectInactiveOutLane() {
        assertThrows(ConflictException.class, () -> parkingCheckOutPolicy.validateLaneForCheckOut(
                lane(LaneDirection.OUT, LaneStatus.CLOSED)
        ));
    }

    @Test
    void shouldRejectInLaneForCheckOut() {
        assertThrows(BadRequestException.class, () -> parkingCheckOutPolicy.validateLaneForCheckOut(
                lane(LaneDirection.IN, LaneStatus.ACTIVE)
        ));
    }

    @Test
    void shouldAllowOnlyInUseCardToExit() {
        assertDoesNotThrow(() -> parkingCheckOutPolicy.validateCardCanExit(card(CardStatus.IN_USE)));
        assertThrows(ConflictException.class, () -> parkingCheckOutPolicy.validateCardCanExit(card(CardStatus.ASSIGNED)));
        assertThrows(ConflictException.class, () -> parkingCheckOutPolicy.validateCardCanExit(card(CardStatus.AVAILABLE)));
    }

    private Card card(CardStatus status) {
        Card card = new Card();
        card.setCardId(UUID.randomUUID());
        card.setCardNumber("C001");
        card.setUid("UID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setVehicleTypeId(UUID.randomUUID());
        card.setStatus(status);
        return card;
    }

    private Lane lane(LaneDirection direction, LaneStatus status) {
        Lane lane = new Lane();
        lane.setLaneId(UUID.randomUUID());
        lane.setGateId(UUID.randomUUID());
        lane.setDirection(direction);
        lane.setStatus(status);
        return lane;
    }

    private Gate gate(GateStatus status) {
        Gate gate = new Gate();
        gate.setGateId(UUID.randomUUID());
        gate.setZoneId(UUID.randomUUID());
        gate.setStatus(status);
        return gate;
    }

    private Zone zone(ZoneStatus status) {
        Zone zone = new Zone();
        zone.setZoneId(UUID.randomUUID());
        zone.setParkingLotId(UUID.randomUUID());
        zone.setVehicleTypeId(UUID.randomUUID());
        zone.setCapacity(100);
        zone.setStatus(status);
        return zone;
    }

    private ParkingLot parkingLot(ParkingLotStatus status) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(UUID.randomUUID());
        parkingLot.setStatus(status);
        return parkingLot;
    }
}
