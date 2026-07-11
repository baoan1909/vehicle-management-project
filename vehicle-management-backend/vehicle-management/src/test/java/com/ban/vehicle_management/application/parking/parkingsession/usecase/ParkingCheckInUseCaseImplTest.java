package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
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
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ParkingCheckInUseCaseImplTest {

    @Mock
    private ParkingSessionAccessGuard parkingSessionAccessGuard;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private CardPortOut cardPortOut;

    @Mock
    private CardTypePortOut cardTypePortOut;

    @Mock
    private SubscriptionPortOut subscriptionPortOut;

    @Mock
    private CustomerPortOut customerPortOut;

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @Mock
    private LanePortOut lanePortOut;

    @Mock
    private GatePortOut gatePortOut;

    @Mock
    private ZonePortOut zonePortOut;

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @Mock
    private ParkingSessionPortOut parkingSessionPortOut;

    @Mock
    private ParkingEventPortOut parkingEventPortOut;

    @Mock
    private FileStoragePort fileStoragePort;

    private ParkingCheckInUseCaseImpl parkingCheckInUseCase;

    @BeforeEach
    void setUp() {
        parkingCheckInUseCase = new ParkingCheckInUseCaseImpl(
                parkingSessionAccessGuard,
                currentAccountPortIn,
                cardPortOut,
                cardTypePortOut,
                subscriptionPortOut,
                customerPortOut,
                customerVehiclePortOut,
                lanePortOut,
                gatePortOut,
                zonePortOut,
                parkingLotPortOut,
                parkingSessionPortOut,
                parkingEventPortOut,
                Mappers.getMapper(ParkingCheckInMapper.class),
                fileStoragePort
        );
    }

    @Test
    void shouldCheckInVisitorCard() {
        TestData data = validTestData();
        Card visitorCard = card(data.cardId(), data.vehicleTypeId(), CardStatus.AVAILABLE);
        MockMultipartFile licensePlateImage = licensePlateImage();
        MockMultipartFile personImage = personImage();
        StoredFile storedLicensePlateFile = storedFile();
        StoredFile storedPersonFile = storedPersonFile();
        CheckInCommand command = new CheckInCommand(
                " UID-001 ",
                data.laneId(),
                data.vehicleTypeId(),
                " 51A-12345 ",
                licensePlateImage,
                personImage,
                " gate 1 "
        );

        mockOperationalTopology(data);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(visitorCard));
        when(cardTypePortOut.findById(visitorCard.getCardTypeId()))
                .thenReturn(Optional.of(cardType(visitorCard.getCardTypeId(), "VISITOR")));
        when(parkingSessionPortOut.existsOpenByCardId(data.cardId())).thenReturn(false);
        when(parkingSessionPortOut.countOpenByZoneId(data.zoneId())).thenReturn(3L);
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSessionPortOut.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class))).thenReturn(storedLicensePlateFile, storedPersonFile);
        when(parkingEventPortOut.save(any(ParkingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(data.actorAccountId());

        CheckInResult result = parkingCheckInUseCase.checkIn(command);

        verify(parkingSessionAccessGuard).ensureCanCheckIn();
        assertEquals(CardStatus.IN_USE, visitorCard.getStatus());
        assertEquals("VISITOR", result.customerType());
        assertNull(result.subscriptionId());
        assertEquals("OPEN", result.barrierAction());
        assertEquals(ParkingSessionStatus.OPEN, result.parkingSession().getStatus());
        assertNull(result.parkingSession().getCustomerId());
        assertEquals(data.cardId(), result.parkingSession().getCardId());
        assertEquals(data.zoneId(), result.parkingSession().getZoneId());
        assertEquals("51A-12345", result.parkingSession().getLicensePlateIn());
        assertEquals(ParkingEventType.CHECK_IN, result.parkingEvent().getEventType());
        assertEquals(data.laneId(), result.parkingEvent().getLaneId());
        assertEquals(data.actorAccountId(), result.parkingEvent().getActorAccountId());
        assertEquals(storedLicensePlateFile.objectKey(), result.parkingEvent().getLicensePlateImagePath());
        assertEquals(storedPersonFile.objectKey(), result.parkingEvent().getPersonImagePath());
        assertEquals("gate 1", result.parkingEvent().getNote());
        verify(fileStoragePort, times(2)).store(any(StoreFileCommand.class));
    }

    @Test
    void shouldUploadParkingEventImagesWhenFilesAreProvided() {
        TestData data = validTestData();
        Card visitorCard = card(data.cardId(), data.vehicleTypeId(), CardStatus.AVAILABLE);
        MockMultipartFile licensePlateImage = licensePlateImage();
        MockMultipartFile personImage = personImage();
        StoredFile storedLicensePlateFile = storedFile();
        StoredFile storedPersonFile = storedPersonFile();
        CheckInCommand command = new CheckInCommand(
                " UID-001 ",
                data.laneId(),
                data.vehicleTypeId(),
                " 51A-12345 ",
                licensePlateImage,
                personImage,
                " gate 1 "
        );

        mockOperationalTopology(data);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(visitorCard));
        when(cardTypePortOut.findById(visitorCard.getCardTypeId()))
                .thenReturn(Optional.of(cardType(visitorCard.getCardTypeId(), "VISITOR")));
        when(parkingSessionPortOut.existsOpenByCardId(data.cardId())).thenReturn(false);
        when(parkingSessionPortOut.countOpenByZoneId(data.zoneId())).thenReturn(3L);
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSessionPortOut.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class))).thenReturn(storedLicensePlateFile, storedPersonFile);
        when(parkingEventPortOut.save(any(ParkingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(data.actorAccountId());

        CheckInResult result = parkingCheckInUseCase.checkIn(command);

        assertEquals(storedLicensePlateFile.objectKey(), result.parkingEvent().getLicensePlateImagePath());
        assertEquals(storedPersonFile.objectKey(), result.parkingEvent().getPersonImagePath());
        verify(fileStoragePort, times(2)).store(any(StoreFileCommand.class));
    }

    @Test
    void shouldCheckInSubscriptionCard() {
        TestData data = validTestData();
        Card subscriptionCard = card(data.cardId(), data.vehicleTypeId(), CardStatus.ASSIGNED);
        Subscription activeSubscription = activeSubscription(data);
        CustomerVehicle customerVehicle = activeCustomerVehicle(data);

        mockOperationalTopology(data);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(subscriptionCard));
        when(cardTypePortOut.findById(subscriptionCard.getCardTypeId()))
                .thenReturn(Optional.of(cardType(subscriptionCard.getCardTypeId(), "REGISTERED")));
        when(parkingSessionPortOut.existsOpenByCardId(data.cardId())).thenReturn(false);
        when(parkingSessionPortOut.countOpenByZoneId(data.zoneId())).thenReturn(3L);
        when(subscriptionPortOut.findActiveByCardId(any(UUID.class), any(LocalDate.class)))
                .thenReturn(Optional.of(activeSubscription));
        when(customerPortOut.findById(data.customerId())).thenReturn(Optional.of(activeApprovedCustomer(data.customerId())));
        when(customerVehiclePortOut.findById(data.customerVehicleId())).thenReturn(Optional.of(customerVehicle));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSessionPortOut.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class))).thenReturn(storedFile(), storedPersonFile());
        when(parkingEventPortOut.save(any(ParkingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(data.actorAccountId());

        CheckInResult result = parkingCheckInUseCase.checkIn(
                new CheckInCommand("UID-001", data.laneId(), data.vehicleTypeId(), "51a-12345", licensePlateImage(), personImage(), null)
        );

        assertEquals(CardStatus.IN_USE, subscriptionCard.getStatus());
        assertEquals("SUBSCRIPTION", result.customerType());
        assertEquals(data.subscriptionId(), result.subscriptionId());
        assertEquals(data.customerId(), result.parkingSession().getCustomerId());
        assertEquals(data.customerVehicleId(), result.parkingSession().getCustomerVehicleId());
        assertEquals(data.vehicleTypeId(), result.parkingSession().getVehicleTypeId());
        assertEquals("51a-12345", result.parkingSession().getLicensePlateIn());
    }

    @Test
    void shouldRejectCheckInFromOutLane() {
        TestData data = validTestData();
        Lane outLane = lane(data.laneId(), data.gateId(), LaneDirection.OUT, LaneStatus.ACTIVE);

        when(lanePortOut.findById(data.laneId())).thenReturn(Optional.of(outLane));

        assertThrows(
                BadRequestException.class,
                () -> parkingCheckInUseCase.checkIn(new CheckInCommand(
                        "UID-001",
                        data.laneId(),
                        data.vehicleTypeId(),
                        "51A-12345",
                        licensePlateImage(),
                        personImage(),
                        null
                ))
        );
        verify(cardPortOut, never()).findByUidForUpdate(any());
    }

    @Test
    void shouldRejectMissingLicensePlateImage() {
        TestData data = validTestData();

        assertThrows(
                BadRequestException.class,
                () -> parkingCheckInUseCase.checkIn(new CheckInCommand(
                        "UID-001",
                        data.laneId(),
                        data.vehicleTypeId(),
                        "51A-12345",
                        null,
                        personImage(),
                        null
                ))
        );
        verify(lanePortOut, never()).findById(any());
        verify(fileStoragePort, never()).store(any(StoreFileCommand.class));
    }

    @Test
    void shouldRejectMissingPersonImage() {
        TestData data = validTestData();

        assertThrows(
                BadRequestException.class,
                () -> parkingCheckInUseCase.checkIn(new CheckInCommand(
                        "UID-001",
                        data.laneId(),
                        data.vehicleTypeId(),
                        "51A-12345",
                        licensePlateImage(),
                        null,
                        null
                ))
        );
        verify(lanePortOut, never()).findById(any());
        verify(fileStoragePort, never()).store(any(StoreFileCommand.class));
    }

    @Test
    void shouldRejectWhenCardAlreadyHasOpenSession() {
        TestData data = validTestData();
        Card visitorCard = card(data.cardId(), data.vehicleTypeId(), CardStatus.AVAILABLE);

        mockOperationalTopology(data);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(visitorCard));
        when(cardTypePortOut.findById(visitorCard.getCardTypeId()))
                .thenReturn(Optional.of(cardType(visitorCard.getCardTypeId(), "VISITOR")));
        when(parkingSessionPortOut.existsOpenByCardId(data.cardId())).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> parkingCheckInUseCase.checkIn(new CheckInCommand(
                        "UID-001",
                        data.laneId(),
                        data.vehicleTypeId(),
                        "51A-12345",
                        licensePlateImage(),
                        personImage(),
                        null
                ))
        );
        verify(cardPortOut, never()).save(any(Card.class));
        verify(parkingSessionPortOut, never()).save(any(ParkingSession.class));
        verify(parkingEventPortOut, never()).save(any(ParkingEvent.class));
    }

    @Test
    void shouldRejectRegisteredCardWhenStatusIsAvailable() {
        TestData data = validTestData();
        Card registeredCard = card(data.cardId(), data.vehicleTypeId(), CardStatus.AVAILABLE);

        mockOperationalTopology(data);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(registeredCard));
        when(cardTypePortOut.findById(registeredCard.getCardTypeId()))
                .thenReturn(Optional.of(cardType(registeredCard.getCardTypeId(), "REGISTERED")));

        assertThrows(
                ConflictException.class,
                () -> parkingCheckInUseCase.checkIn(new CheckInCommand(
                        "UID-001",
                        data.laneId(),
                        data.vehicleTypeId(),
                        "51A-12345",
                        licensePlateImage(),
                        personImage(),
                        null
                ))
        );
        verify(subscriptionPortOut, never()).findActiveByCardId(any(UUID.class), any(LocalDate.class));
        verify(cardPortOut, never()).save(any(Card.class));
        verify(parkingSessionPortOut, never()).save(any(ParkingSession.class));
        verify(parkingEventPortOut, never()).save(any(ParkingEvent.class));
        verify(fileStoragePort, never()).store(any(StoreFileCommand.class));
    }

    private MockMultipartFile licensePlateImage() {
        return new MockMultipartFile(
                "licensePlateImage",
                "plate.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );
    }

    private MockMultipartFile personImage() {
        return new MockMultipartFile(
                "personImage",
                "person.jpg",
                "image/jpeg",
                new byte[] {4, 5, 6}
        );
    }

    private StoredFile storedFile() {
        return new StoredFile(
                "pe/2026/06/26/event/pv-generated-parking-event.jpg",
                "plate.jpg",
                "image/jpeg",
                3,
                "checksum"
        );
    }

    private StoredFile storedPersonFile() {
        return new StoredFile(
                "pe/2026/06/26/event/pv-generated-person.jpg",
                "person.jpg",
                "image/jpeg",
                3,
                "checksum-person"
        );
    }

    private CardType cardType(UUID cardTypeId, String code) {
        CardType cardType = new CardType();
        cardType.setCardTypeId(cardTypeId);
        cardType.setCode(code);
        cardType.setName(code);
        cardType.setIsActive(Boolean.TRUE);
        return cardType;
    }

    private void mockOperationalTopology(TestData data) {
        when(lanePortOut.findById(data.laneId()))
                .thenReturn(Optional.of(lane(data.laneId(), data.gateId(), LaneDirection.IN, LaneStatus.ACTIVE)));
        when(gatePortOut.findById(data.gateId()))
                .thenReturn(Optional.of(gate(data.gateId(), data.zoneId(), GateStatus.ACTIVE)));
        when(zonePortOut.findById(data.zoneId()))
                .thenReturn(Optional.of(zone(data.zoneId(), data.parkingLotId(), data.vehicleTypeId(), 100, ZoneStatus.ACTIVE)));
        when(parkingLotPortOut.findById(data.parkingLotId()))
                .thenReturn(Optional.of(parkingLot(data.parkingLotId(), ParkingLotStatus.ACTIVE)));
    }

    private Card card(UUID cardId, UUID vehicleTypeId, CardStatus status) {
        Card card = new Card();
        card.setCardId(cardId);
        card.setCardNumber("C001");
        card.setUid("UID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setStatus(status);
        return card;
    }

    private Subscription activeSubscription(TestData data) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(data.subscriptionId());
        subscription.setCustomerId(data.customerId());
        subscription.setCustomerVehicleId(data.customerVehicleId());
        subscription.setCardId(data.cardId());
        subscription.setTicketTypeId(UUID.randomUUID());
        subscription.setPriceRuleId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setEffectiveFrom(LocalDate.of(2026, 6, 1));
        subscription.setEffectiveTo(LocalDate.of(2026, 6, 30));
        return subscription;
    }

    private CustomerVehicle activeCustomerVehicle(TestData data) {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerVehicleId(data.customerVehicleId());
        customerVehicle.setCustomerId(data.customerId());
        customerVehicle.setVehicleTypeId(data.vehicleTypeId());
        customerVehicle.setLicensePlate("51A-12345");
        customerVehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        return customerVehicle;
    }

    private Customer activeApprovedCustomer(UUID customerId) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.APPROVED);
        return customer;
    }

    private Lane lane(UUID laneId, UUID gateId, LaneDirection direction, LaneStatus status) {
        Lane lane = new Lane();
        lane.setLaneId(laneId);
        lane.setGateId(gateId);
        lane.setDirection(direction);
        lane.setStatus(status);
        return lane;
    }

    private Gate gate(UUID gateId, UUID zoneId, GateStatus status) {
        Gate gate = new Gate();
        gate.setGateId(gateId);
        gate.setZoneId(zoneId);
        gate.setStatus(status);
        return gate;
    }

    private Zone zone(UUID zoneId, UUID parkingLotId, UUID vehicleTypeId, int capacity, ZoneStatus status) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(parkingLotId);
        zone.setVehicleTypeId(vehicleTypeId);
        zone.setCapacity(capacity);
        zone.setStatus(status);
        return zone;
    }

    private ParkingLot parkingLot(UUID parkingLotId, ParkingLotStatus status) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(parkingLotId);
        parkingLot.setStatus(status);
        return parkingLot;
    }

    private TestData validTestData() {
        return new TestData(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }

    private record TestData(
            UUID cardId,
            UUID vehicleTypeId,
            UUID laneId,
            UUID gateId,
            UUID zoneId,
            UUID parkingLotId,
            UUID subscriptionId,
            UUID customerId,
            UUID customerVehicleId,
            UUID actorAccountId
    ) {
    }
}
