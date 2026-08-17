package com.ban.vehicle_management.domain.parking.parkingsession.policy;

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

public class ParkingCheckOutPolicy {

    public void validateLaneForCheckOut(Lane lane) {
        requireField(lane, "lane");
        if (lane.getStatus() != LaneStatus.ACTIVE) {
            throw new ConflictException("Làn xe hiện không hoạt động");
        }
        if (lane.getDirection() != LaneDirection.OUT) {
            throw new BadRequestException("Vui lòng chọn đúng làn xe ra");
        }
    }

    public void validateOperationalTopology(Lane lane, Gate gate, Zone zone, ParkingLot parkingLot) {
        requireField(lane, "lane");
        requireField(gate, "gate");
        requireField(zone, "zone");
        requireField(parkingLot, "parkingLot");

        validateLaneForCheckOut(lane);
        if (gate.getStatus() != GateStatus.ACTIVE) {
            throw new ConflictException("Cổng xe hiện không hoạt động");
        }
        if (zone.getStatus() != ZoneStatus.ACTIVE) {
            throw new ConflictException("Khu vực đỗ xe hiện không hoạt động");
        }
        if (parkingLot.getStatus() != ParkingLotStatus.ACTIVE) {
            throw new ConflictException("Bãi xe hiện không hoạt động");
        }
    }

    public void validateCardCanExit(Card card) {
        requireField(card, "card");
        requireField(card.getCardId(), "cardId");
        requireField(card.getStatus(), "cardStatus");

        if (card.getStatus() != CardStatus.IN_USE) {
            throw new ConflictException("Thẻ hiện không được sử dụng trong phiên gửi xe");
        }
    }


    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException("Thiếu thông tin bắt buộc: " + fieldName);
        }
    }
}
