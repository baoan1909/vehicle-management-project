package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.UUID;

public class ParkingCheckInPolicy {

    private static final String CARD_TYPE_VISITOR = "VISITOR";
    private static final String CARD_TYPE_REGISTERED = "REGISTERED";

    private final ParkingLicensePlatePolicy licensePlatePolicy = new ParkingLicensePlatePolicy();

    public void validateLaneForCheckIn(Lane lane) {
        requireField(lane, "lane");
        if (lane.getStatus() != LaneStatus.ACTIVE) {
            throw new ConflictException("Làn xe hiện không hoạt động");
        }
        if (lane.getDirection() != LaneDirection.IN) {
            throw new BadRequestException("Vui lòng chọn đúng làn xe vào");
        }
    }

    public void validateOperationalTopology(Lane lane, Gate gate, Zone zone, ParkingLot parkingLot) {
        requireField(lane, "lane");
        requireField(gate, "gate");
        requireField(zone, "zone");
        requireField(parkingLot, "parkingLot");

        validateLaneForCheckIn(lane);
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

    public void validateCardCanEnter(Card card, CardType cardType) {
        requireField(card, "card");
        requireField(card.getCardId(), "cardId");
        requireField(card.getCardTypeId(), "cardTypeId");
        requireField(card.getStatus(), "cardStatus");
        requireField(cardType, "cardType");
        requireField(cardType.getCode(), "cardTypeCode");

        if (isVisitorCard(cardType)) {
            if (card.getStatus() != CardStatus.AVAILABLE) {
                throw new ConflictException("Thẻ vãng lai phải ở trạng thái sẵn sàng để ghi nhận xe vào");
            }
            return;
        }

        if (isSubscriptionCard(cardType)) {
            if (card.getStatus() != CardStatus.ASSIGNED) {
                throw new ConflictException("Thẻ đăng ký phải được gán trước khi ghi nhận xe vào");
            }
            return;
        }

        throw new ConflictException("Loại thẻ này không được phép sử dụng để ghi nhận xe vào");
    }

    public boolean isVisitorCard(CardType cardType) {
        requireField(cardType, "cardType");
        return CARD_TYPE_VISITOR.equalsIgnoreCase(cardType.getCode());
    }

    public boolean isSubscriptionCard(CardType cardType) {
        requireField(cardType, "cardType");
        return CARD_TYPE_REGISTERED.equalsIgnoreCase(cardType.getCode());
    }

    public void validateVehicleTypeAccepted(UUID vehicleTypeId, Zone zone) {
        requireField(vehicleTypeId, "vehicleTypeId");
        requireField(zone, "zone");

        if (zone.getVehicleTypeId() != null && !zone.getVehicleTypeId().equals(vehicleTypeId)) {
            throw new ConflictException("Loại xe này không được phép vào khu vực đã chọn");
        }
    }

    public void ensureCardHasNoOpenSession(boolean hasOpenSession) {
        if (hasOpenSession) {
            throw new ConflictException("Thẻ đang có một phiên gửi xe chưa kết thúc");
        }
    }

    public void validateZoneCapacity(Zone zone, long openSessionCount) {
        requireField(zone, "zone");
        if (zone.getCapacity() == null || zone.getCapacity() <= 0) {
            throw new ConflictException("Khu vực đỗ xe không còn sức chứa khả dụng");
        }
        if (openSessionCount >= zone.getCapacity()) {
            throw new ConflictException("Khu vực đỗ xe đã hết chỗ");
        }
    }

    public void validateSubscriptionContext(
            Card card,
            Subscription subscription,
            Customer customer,
            CustomerVehicle customerVehicle,
            String detectedLicensePlate
    ) {
        requireField(card, "card");
        requireField(subscription, "subscription");
        requireField(customer, "customer");
        requireField(customerVehicle, "customerVehicle");

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new ConflictException("Khách hàng hiện không hoạt động");
        }
        if (customer.getApprovalStatus() != CustomerApprovalStatus.APPROVED) {
            throw new ConflictException("Khách hàng chưa được phê duyệt");
        }
        if (!subscription.getCustomerId().equals(customer.getCustomerId())) {
            throw new ConflictException("Vé đăng ký không thuộc khách hàng này");
        }
        if (!subscription.getCustomerId().equals(customerVehicle.getCustomerId())) {
            throw new ConflictException("Vé đăng ký không thuộc phương tiện này");
        }
        if (customerVehicle.getStatus() != CustomerVehicleStatus.ACTIVE) {
            throw new ConflictException("Phương tiện của khách hàng hiện không hoạt động");
        }
        if (!licensePlatePolicy.matches(customerVehicle.getLicensePlate(), detectedLicensePlate)) {
            throw new ConflictException("Biển số nhận diện không khớp với phương tiện đã đăng ký");
        }
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException("Thiếu thông tin bắt buộc: " + fieldName);
        }
    }
}
