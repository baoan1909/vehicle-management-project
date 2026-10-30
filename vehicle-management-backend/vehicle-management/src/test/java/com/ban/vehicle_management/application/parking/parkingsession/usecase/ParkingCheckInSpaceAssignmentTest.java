package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.catalog.cardtype.port.out.CardTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.parkingevent.port.out.ParkingEventPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingCheckInMapper;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.catalog.cardtype.model.CardType;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ParkingCheckInSpaceAssignmentTest {

    @Mock private ParkingSessionAccessGuard parkingSessionAccessGuard;
    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private CardPortOut cardPortOut;
    @Mock private CardTypePortOut cardTypePortOut;
    @Mock private SubscriptionPortOut subscriptionPortOut;
    @Mock private CustomerPortOut customerPortOut;
    @Mock private CustomerVehiclePortOut customerVehiclePortOut;
    @Mock private LanePortOut lanePortOut;
    @Mock private GatePortOut gatePortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ParkingLotCatalogAvailabilityPortOut catalogAvailabilityPortOut;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;
    @Mock private ParkingEventPortOut parkingEventPortOut;
    @Mock private FileStoragePort fileStoragePort;
    @Mock private ParkingSpacePortOut parkingSpacePortOut;
    @Mock private ParkingSpaceAllocationPortOut allocationPortOut;

    private ParkingCheckInUseCaseImpl useCase;

    private final UUID cardId = UUID.randomUUID();
    private final UUID vehicleTypeId = UUID.randomUUID();
    private final UUID laneId = UUID.randomUUID();
    private final UUID gateId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID lotId = UUID.randomUUID();
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ParkingCheckInUseCaseImpl(
                parkingSessionAccessGuard, currentAccountPortIn, cardPortOut, cardTypePortOut,
                subscriptionPortOut, customerPortOut, customerVehiclePortOut, lanePortOut, gatePortOut,
                zonePortOut, parkingLotPortOut, catalogAvailabilityPortOut, parkingSessionPortOut,
                parkingEventPortOut, Mappers.getMapper(ParkingCheckInMapper.class), fileStoragePort,
                parkingSpacePortOut, allocationPortOut, null);
        lenient().when(catalogAvailabilityPortOut.isVehicleTypeEnabled(any(UUID.class), any(UUID.class)))
                .thenReturn(true);
    }

    @Test
    void shouldBindAvailableSpaceAndMarkOccupiedVisitor() {
        mockSpaceTopology();
        mockVisitorCard();
        ParkingSpace space = availableSpace(spaceId, zoneId);
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(space));
        when(parkingSessionPortOut.existsOpenByParkingSpaceId(spaceId)).thenReturn(false);
        when(allocationPortOut.findActiveAllocationsForSpaceOnDate(eq(spaceId), any(LocalDate.class)))
                .thenReturn(List.of());
        when(parkingSessionPortOut.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSpacePortOut.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(storedFile("plate.jpg"), storedFile("person.jpg"));
        when(parkingEventPortOut.save(any(ParkingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());

        CheckInResult result = useCase.checkIn(commandWithSpace(spaceId));

        assertEquals(spaceId, result.parkingSession().getParkingSpaceId());
        assertEquals(ParkingSpaceStatus.OCCUPIED, space.getStatus());
        assertEquals(ParkingSpaceStatusSource.AUTOMATIC, space.getStatusSource());
        verify(parkingSpacePortOut).save(space);
    }

    @Test
    void shouldAutoAssignAvailableSpaceForVisitorInSpaceZone() {
        mockSpaceTopology();
        mockVisitorCard();
        ParkingSpace space = availableSpace(spaceId, zoneId);
        when(parkingSpacePortOut.findFirstVisitorAvailableForUpdate(
                eq(zoneId), eq(vehicleTypeId), any(Instant.class)))
                .thenReturn(Optional.of(space));
        when(parkingSessionPortOut.existsOpenByParkingSpaceId(spaceId)).thenReturn(false);
        when(allocationPortOut.findActiveAllocationsForSpaceOnDate(eq(spaceId), any(LocalDate.class)))
                .thenReturn(List.of());
        when(parkingSessionPortOut.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSpacePortOut.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(storedFile("plate.jpg"), storedFile("person.jpg"));
        when(parkingEventPortOut.save(any(ParkingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());

        CheckInResult result = useCase.checkIn(commandWithSpace(null));

        assertEquals(spaceId, result.parkingSession().getParkingSpaceId());
        assertEquals("A-01", result.parkingSpaceCode());
        assertEquals(ParkingSpaceStatus.OCCUPIED, space.getStatus());
        verify(parkingSpacePortOut).save(space);
        verify(parkingSpacePortOut, never()).findByIdForUpdate(spaceId);
    }

    @Test
    void shouldRejectVisitorWhenNoCompatibleSpaceCanBeAutoAssigned() {
        mockSpaceTopology();
        mockVisitorCard();
        when(parkingSpacePortOut.findFirstVisitorAvailableForUpdate(
                eq(zoneId), eq(vehicleTypeId), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> useCase.checkIn(commandWithSpace(null)));

        verify(parkingSessionPortOut, never()).save(any(ParkingSession.class));
    }

    @Test
    void shouldRejectOccupiedSpace() {
        mockSpaceTopology();
        mockVisitorCard();
        ParkingSpace space = availableSpace(spaceId, zoneId);
        space.setStatus(ParkingSpaceStatus.OCCUPIED);
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(space));

        assertThrows(ConflictException.class, () -> useCase.checkIn(commandWithSpace(spaceId)));
        verify(parkingSessionPortOut, never()).save(any(ParkingSession.class));
    }

    @Test
    void shouldRejectVisitorSpaceWithSubscriptionAllocation() {
        mockSpaceTopology();
        mockVisitorCard();
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(availableSpace(spaceId, zoneId)));
        when(parkingSessionPortOut.existsOpenByParkingSpaceId(spaceId)).thenReturn(false);
        ParkingSpaceAllocation other = new ParkingSpaceAllocation();
        other.setSubscriptionId(UUID.randomUUID());
        other.setAllocationStatus(ParkingSpaceAllocationStatus.ACTIVE);
        when(allocationPortOut.findActiveAllocationsForSpaceOnDate(eq(spaceId), any(LocalDate.class)))
                .thenReturn(List.of(other));

        assertThrows(ConflictException.class, () -> useCase.checkIn(commandWithSpace(spaceId)));
        verify(parkingSessionPortOut, never()).save(any(ParkingSession.class));
    }

    @Test
    void shouldRejectSpaceFromAnotherZone() {
        mockSpaceTopology();
        mockVisitorCard();
        when(parkingSpacePortOut.findById(spaceId))
                .thenReturn(Optional.of(availableSpace(spaceId, UUID.randomUUID())));

        assertThrows(BadRequestException.class, () -> useCase.checkIn(commandWithSpace(spaceId)));
        verify(parkingSessionPortOut, never()).save(any(ParkingSession.class));
    }

    @Test
    void shouldUseOwnReservedSpaceForRegisteredSubscription() {
        mockSpaceTopology();
        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Card card = new Card();
        card.setCardId(cardId);
        card.setCardNumber("C001");
        card.setUid("UID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setStatus(CardStatus.ASSIGNED);
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(subscriptionId);
        subscription.setParkingLotId(lotId);
        subscription.setCardId(cardId);
        subscription.setCustomerId(customerId);
        subscription.setCustomerVehicleId(vehicleId);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setEffectiveFrom(LocalDate.now().minusDays(1));
        subscription.setEffectiveTo(LocalDate.now().plusDays(30));
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.APPROVED);
        CustomerVehicle vehicle = new CustomerVehicle();
        vehicle.setCustomerVehicleId(vehicleId);
        vehicle.setCustomerId(customerId);
        vehicle.setVehicleTypeId(vehicleTypeId);
        vehicle.setLicensePlate("51A-12345");
        vehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        ParkingSpace reserved = availableSpace(spaceId, zoneId);
        reserved.setStatus(ParkingSpaceStatus.RESERVED);
        ParkingSpaceAllocation allocation = new ParkingSpaceAllocation();
        allocation.setParkingSpaceId(spaceId);
        allocation.setSubscriptionId(subscriptionId);
        allocation.setEffectiveFrom(LocalDate.now().minusDays(1));
        allocation.setEffectiveTo(LocalDate.now().plusDays(30));
        allocation.setAllocationStatus(ParkingSpaceAllocationStatus.ACTIVE);

        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(card));
        when(cardTypePortOut.findById(card.getCardTypeId()))
                .thenReturn(Optional.of(cardType(card.getCardTypeId(), "REGISTERED")));
        when(parkingSessionPortOut.existsOpenByCardId(cardId)).thenReturn(false);
        when(subscriptionPortOut.findActiveByCardIdForUpdate(eq(cardId), any(LocalDate.class)))
                .thenReturn(Optional.of(subscription));
        when(subscriptionPortOut.findOperationalVehicleTypeIdBySubscriptionId(subscriptionId))
                .thenReturn(Optional.of(vehicleTypeId));
        when(customerPortOut.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerVehiclePortOut.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(allocationPortOut.findActiveAllocationsBySubscription(subscriptionId))
                .thenReturn(List.of(allocation));
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(reserved));
        when(parkingSessionPortOut.existsOpenByParkingSpaceId(spaceId)).thenReturn(false);
        when(allocationPortOut.findActiveAllocationsForSpaceOnDate(eq(spaceId), any(LocalDate.class)))
                .thenReturn(List.of(allocation));
        when(parkingSessionPortOut.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSpacePortOut.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(storedFile("plate.jpg"), storedFile("person.jpg"));
        when(parkingEventPortOut.save(any(ParkingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());

        CheckInResult result = useCase.checkIn(commandWithSpace(null));

        assertEquals(spaceId, result.parkingSession().getParkingSpaceId());
        assertEquals(ParkingSpaceStatus.OCCUPIED, reserved.getStatus());
    }

    private CheckInCommand commandWithSpace(UUID parkingSpaceId) {
        return new CheckInCommand("UID-001", laneId, vehicleTypeId, "51A-12345",
                image("plate.jpg"), image("person.jpg"), null, false, false, parkingSpaceId);
    }

    private void mockSpaceTopology() {
        Lane lane = new Lane();
        lane.setLaneId(laneId);
        lane.setGateId(gateId);
        lane.setDirection(LaneDirection.IN);
        lane.setStatus(LaneStatus.ACTIVE);
        Gate gate = new Gate();
        gate.setGateId(gateId);
        gate.setZoneId(zoneId);
        gate.setStatus(GateStatus.ACTIVE);
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(lotId);
        zone.setVehicleTypeId(vehicleTypeId);
        zone.setCapacity(100);
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setTrackingMode(TrackingMode.SPACE);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setStatus(ParkingLotStatus.ACTIVE);
        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(lane));
        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(gate));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
        lenient().when(parkingSessionPortOut.countOpenByZoneId(zoneId)).thenReturn(0L);
    }

    private void mockVisitorCard() {
        Card card = new Card();
        card.setCardId(cardId);
        card.setCardNumber("C001");
        card.setUid("UID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setStatus(CardStatus.AVAILABLE);
        CardType cardType = new CardType();
        cardType.setCardTypeId(card.getCardTypeId());
        cardType.setCode("VISITOR");
        cardType.setName("VISITOR");
        cardType.setIsActive(Boolean.TRUE);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(card));
        when(cardTypePortOut.findById(card.getCardTypeId())).thenReturn(Optional.of(cardType));
        when(parkingSessionPortOut.existsOpenByCardId(cardId)).thenReturn(false);
        lenient().when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CardType cardType(UUID id, String code) {
        CardType cardType = new CardType();
        cardType.setCardTypeId(id);
        cardType.setCode(code);
        cardType.setName(code);
        cardType.setIsActive(Boolean.TRUE);
        return cardType;
    }

    private ParkingSpace availableSpace(UUID id, UUID zone) {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(id);
        space.setZoneId(zone);
        space.setCode("A-01");
        space.setVehicleTypeId(vehicleTypeId);
        space.setLifecycleStatus("ACTIVE");
        space.setStatus(ParkingSpaceStatus.AVAILABLE);
        space.setStatusSource(ParkingSpaceStatusSource.AUTOMATIC);
        space.setVersion(0L);
        return space;
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile("image", name, "image/jpeg", new byte[] {1, 2, 3});
    }

    private StoredFile storedFile(String name) {
        return new StoredFile("pe/key/" + name, name, "image/jpeg", 3, "checksum");
    }

}
