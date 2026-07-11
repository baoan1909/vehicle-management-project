package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Test;

class ParkingCheckInPolicyTest {

    private final ParkingCheckInPolicy parkingCheckInPolicy = new ParkingCheckInPolicy();

    @Test
    void shouldAcceptOperationalInTopology() {
        assertDoesNotThrow(() -> parkingCheckInPolicy.validateOperationalTopology(
                lane(LaneDirection.IN, LaneStatus.ACTIVE),
                gate(GateStatus.ACTIVE),
                zone(UUID.randomUUID(), 100, ZoneStatus.ACTIVE),
                parkingLot(ParkingLotStatus.ACTIVE)
        ));
    }

    @Test
    void shouldRejectOutLaneForCheckIn() {
        assertThrows(ConflictException.class, () -> parkingCheckInPolicy.validateLaneForCheckIn(
                lane(LaneDirection.IN, LaneStatus.CLOSED)
        ));

        assertThrows(BadRequestException.class, () -> parkingCheckInPolicy.validateLaneForCheckIn(
                lane(LaneDirection.OUT, LaneStatus.ACTIVE)
        ));
    }

    @Test
    void shouldAllowAvailableAndAssignedCardsOnly() {
        Card availableCard = card(CardStatus.AVAILABLE, UUID.randomUUID());
        Card assignedCard = card(CardStatus.ASSIGNED, UUID.randomUUID());
        Card inUseCard = card(CardStatus.IN_USE, UUID.randomUUID());
        CardType visitorCardType = cardType("VISITOR");
        CardType registeredCardType = cardType("REGISTERED");

        assertDoesNotThrow(() -> parkingCheckInPolicy.validateCardCanEnter(availableCard, visitorCardType));
        assertDoesNotThrow(() -> parkingCheckInPolicy.validateCardCanEnter(assignedCard, registeredCardType));
        assertThrows(ConflictException.class, () -> parkingCheckInPolicy.validateCardCanEnter(inUseCard, visitorCardType));
        assertThrows(ConflictException.class, () -> parkingCheckInPolicy.validateCardCanEnter(availableCard, registeredCardType));
        assertTrue(parkingCheckInPolicy.isVisitorCard(visitorCardType));
        assertTrue(parkingCheckInPolicy.isSubscriptionCard(registeredCardType));
        assertFalse(parkingCheckInPolicy.isVisitorCard(registeredCardType));
    }

    @Test
    void shouldRejectFullZoneCapacity() {
        Zone zone = zone(UUID.randomUUID(), 2, ZoneStatus.ACTIVE);

        assertDoesNotThrow(() -> parkingCheckInPolicy.validateZoneCapacity(zone, 1));
        assertThrows(ConflictException.class, () -> parkingCheckInPolicy.validateZoneCapacity(zone, 2));
    }

    @Test
    void shouldValidateSubscriptionContext() {
        UUID customerId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Card card = card(CardStatus.ASSIGNED, vehicleTypeId);
        Subscription subscription = subscription(customerId, UUID.randomUUID());
        Customer customer = customer(customerId, CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED);
        CustomerVehicle customerVehicle = customerVehicle(
                subscription.getCustomerVehicleId(),
                customerId,
                vehicleTypeId,
                "51A-12345",
                CustomerVehicleStatus.ACTIVE
        );

        assertDoesNotThrow(() -> parkingCheckInPolicy.validateSubscriptionContext(
                card,
                subscription,
                customer,
                customerVehicle,
                "51a-12345"
        ));
    }

    @Test
    void shouldRejectSubscriptionContextWhenPlateDoesNotMatch() {
        UUID customerId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Card card = card(CardStatus.ASSIGNED, vehicleTypeId);
        Subscription subscription = subscription(customerId, UUID.randomUUID());
        Customer customer = customer(customerId, CustomerStatus.ACTIVE, CustomerApprovalStatus.APPROVED);
        CustomerVehicle customerVehicle = customerVehicle(
                subscription.getCustomerVehicleId(),
                customerId,
                vehicleTypeId,
                "51A-12345",
                CustomerVehicleStatus.ACTIVE
        );

        assertThrows(ConflictException.class, () -> parkingCheckInPolicy.validateSubscriptionContext(
                card,
                subscription,
                customer,
                customerVehicle,
                "51A-99999"
        ));
    }

    private Card card(CardStatus status, UUID vehicleTypeId) {
        Card card = new Card();
        card.setCardId(UUID.randomUUID());
        card.setCardNumber("C001");
        card.setUid("UID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setStatus(status);
        return card;
    }

    private CardType cardType(String code) {
        CardType cardType = new CardType();
        cardType.setCardTypeId(UUID.randomUUID());
        cardType.setCode(code);
        cardType.setName(code);
        cardType.setIsActive(Boolean.TRUE);
        return cardType;
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

    private Zone zone(UUID vehicleTypeId, int capacity, ZoneStatus status) {
        Zone zone = new Zone();
        zone.setZoneId(UUID.randomUUID());
        zone.setParkingLotId(UUID.randomUUID());
        zone.setVehicleTypeId(vehicleTypeId);
        zone.setCapacity(capacity);
        zone.setStatus(status);
        return zone;
    }

    private ParkingLot parkingLot(ParkingLotStatus status) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(UUID.randomUUID());
        parkingLot.setStatus(status);
        return parkingLot;
    }

    private Subscription subscription(UUID customerId, UUID customerVehicleId) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setCustomerId(customerId);
        subscription.setCustomerVehicleId(customerVehicleId);
        subscription.setCardId(UUID.randomUUID());
        return subscription;
    }

    private Customer customer(UUID customerId, CustomerStatus status, CustomerApprovalStatus approvalStatus) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setStatus(status);
        customer.setApprovalStatus(approvalStatus);
        return customer;
    }

    private CustomerVehicle customerVehicle(
            UUID customerVehicleId,
            UUID customerId,
            UUID vehicleTypeId,
            String licensePlate,
            CustomerVehicleStatus status
    ) {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerVehicleId(customerVehicleId);
        customerVehicle.setCustomerId(customerId);
        customerVehicle.setVehicleTypeId(vehicleTypeId);
        customerVehicle.setLicensePlate(licensePlate);
        customerVehicle.setStatus(status);
        return customerVehicle;
    }
}
