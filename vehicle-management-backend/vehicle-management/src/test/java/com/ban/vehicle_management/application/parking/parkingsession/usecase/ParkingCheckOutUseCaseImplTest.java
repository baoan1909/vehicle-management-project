package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.parkingevent.port.out.ParkingEventPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingCheckOutMapper;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckOutCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
class ParkingCheckOutUseCaseImplTest {

    @Mock
    private ParkingSessionAccessGuard parkingSessionAccessGuard;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private CardPortOut cardPortOut;

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
    private PriceRulePortOut priceRulePortOut;

    @Mock
    private InvoicePortOut invoicePortOut;

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private FileAccessPort fileAccessPort;

    private ParkingCheckOutUseCaseImpl parkingCheckOutUseCase;

    @BeforeEach
    void setUp() {
        parkingCheckOutUseCase = new ParkingCheckOutUseCaseImpl(
                parkingSessionAccessGuard,
                currentAccountPortIn,
                cardPortOut,
                lanePortOut,
                gatePortOut,
                zonePortOut,
                parkingLotPortOut,
                parkingSessionPortOut,
                parkingEventPortOut,
                priceRulePortOut,
                invoicePortOut,
                Mappers.getMapper(ParkingCheckOutMapper.class),
                fileStoragePort,
                fileAccessPort
        );
    }

    @Test
    void shouldCheckOutVisitorSessionAndCreateUnpaidInvoice() {
        TestData data = validTestData();
        Card card = card(data.cardId(), data.vehicleTypeId(), CardStatus.IN_USE);
        ParkingSession openSession = visitorOpenSession(data, Instant.now().minusSeconds(2 * 60 * 60));
        StoredFile storedLicensePlateFile = storedFile();
        StoredFile storedPersonFile = storedPersonFile();

        mockOperationalTopology(data);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(card));
        when(parkingSessionPortOut.findOpenByCardId(data.cardId())).thenReturn(Optional.of(openSession));
        when(priceRulePortOut.findActiveVisitorRuleByTime(eq(data.vehicleTypeId()), any(LocalDate.class), eq(LocalTime.NOON)))
                .thenReturn(Optional.of(priceRule(data.vehicleTypeId(), new BigDecimal("5000"), LocalTime.of(6, 0), LocalTime.of(19, 59, 59))));
        when(priceRulePortOut.findActiveVisitorRuleByTime(eq(data.vehicleTypeId()), any(LocalDate.class), eq(LocalTime.MIDNIGHT)))
                .thenReturn(Optional.of(priceRule(data.vehicleTypeId(), new BigDecimal("10000"), LocalTime.of(20, 0), LocalTime.of(5, 59, 59))));
        when(parkingSessionPortOut.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class))).thenReturn(storedLicensePlateFile, storedPersonFile);
        when(parkingEventPortOut.save(any(ParkingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePortOut.existsByParkingSessionIdAndStatusIn(eq(data.parkingSessionId()), anyCollection()))
                .thenReturn(false);
        when(invoicePortOut.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(data.actorAccountId());

        CheckOutResult result = parkingCheckOutUseCase.checkOut(command(data.laneId(), "51A-12345"));

        verify(parkingSessionAccessGuard).ensureCanCheckOut();
        assertEquals(ParkingSessionStatus.CLOSED, result.parkingSession().getStatus());
        assertEquals("51A-12345", result.parkingSession().getLicensePlateOut());
        assertEquals(new BigDecimal("5000"), result.parkingSession().getTotalPrice());
        assertEquals(CardStatus.AVAILABLE, card.getStatus());
        assertEquals("VISITOR", result.customerType());
        assertEquals("WAIT_PAYMENT", result.barrierAction());
        assertNotNull(result.invoice());
        assertEquals(InvoiceStatus.UNPAID, result.invoice().getStatus());
        assertEquals(data.parkingSessionId(), result.invoice().getParkingSessionId());
        assertEquals(new BigDecimal("5000"), result.invoice().getAmount());
        assertEquals(ParkingEventType.CHECK_OUT, result.parkingEvent().getEventType());
        assertEquals(storedLicensePlateFile.objectKey(), result.parkingEvent().getLicensePlateImagePath());
        assertEquals(storedPersonFile.objectKey(), result.parkingEvent().getPersonImagePath());
        verify(fileStoragePort, times(2)).store(any(StoreFileCommand.class));
    }

    @Test
    void shouldCheckOutSubscriptionSessionWithoutInvoice() {
        TestData data = validTestData();
        Card card = card(data.cardId(), data.vehicleTypeId(), CardStatus.IN_USE);
        ParkingSession openSession = subscriptionOpenSession(data);

        mockOperationalTopology(data);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(card));
        when(parkingSessionPortOut.findOpenByCardId(data.cardId())).thenReturn(Optional.of(openSession));
        when(parkingSessionPortOut.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class))).thenReturn(storedFile(), storedPersonFile());
        when(parkingEventPortOut.save(any(ParkingEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(data.actorAccountId());

        CheckOutResult result = parkingCheckOutUseCase.checkOut(command(data.laneId(), "51A-12345"));

        assertEquals(ParkingSessionStatus.CLOSED, result.parkingSession().getStatus());
        assertEquals(BigDecimal.ZERO, result.parkingSession().getTotalPrice());
        assertEquals(CardStatus.ASSIGNED, card.getStatus());
        assertEquals("SUBSCRIPTION", result.customerType());
        assertEquals("OPEN", result.barrierAction());
        assertNull(result.invoice());
        verify(priceRulePortOut, never()).findActiveVisitorRuleByTime(any(), any(), any());
        verify(invoicePortOut, never()).save(any(Invoice.class));
    }

    @Test
    void shouldRejectWhenLicensePlateDoesNotMatchOpenSession() {
        TestData data = validTestData();
        Card card = card(data.cardId(), data.vehicleTypeId(), CardStatus.IN_USE);
        ParkingSession openSession = visitorOpenSession(data, Instant.now().minusSeconds(60 * 60));

        mockOperationalTopology(data);
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(card));
        when(parkingSessionPortOut.findOpenByCardId(data.cardId())).thenReturn(Optional.of(openSession));

        assertThrows(
                ConflictException.class,
                () -> parkingCheckOutUseCase.checkOut(command(data.laneId(), "51A-99999"))
        );

        verify(parkingSessionPortOut, never()).save(any(ParkingSession.class));
        verify(cardPortOut, never()).save(any(Card.class));
        verify(fileStoragePort, never()).store(any(StoreFileCommand.class));
        verify(invoicePortOut, never()).save(any(Invoice.class));
    }

    private CheckOutCommand command(UUID laneId, String licensePlate) {
        return new CheckOutCommand(
                laneId,
                " UID-001 ",
                licensePlate,
                licensePlateImage(),
                personImage(),
                " checkout "
        );
    }

    private void mockOperationalTopology(TestData data) {
        when(lanePortOut.findById(data.laneId()))
                .thenReturn(Optional.of(lane(data.laneId(), data.gateId(), LaneDirection.OUT, LaneStatus.ACTIVE)));
        when(gatePortOut.findById(data.gateId()))
                .thenReturn(Optional.of(gate(data.gateId(), data.zoneId(), GateStatus.ACTIVE)));
        when(zonePortOut.findById(data.zoneId()))
                .thenReturn(Optional.of(zone(data.zoneId(), data.parkingLotId(), data.vehicleTypeId(), ZoneStatus.ACTIVE)));
        when(parkingLotPortOut.findById(data.parkingLotId()))
                .thenReturn(Optional.of(parkingLot(data.parkingLotId(), ParkingLotStatus.ACTIVE)));
    }

    private ParkingSession visitorOpenSession(TestData data, Instant checkInTime) {
        ParkingSession parkingSession = baseOpenSession(data, checkInTime);
        parkingSession.setCustomerId(null);
        parkingSession.setCustomerVehicleId(null);
        return parkingSession;
    }

    private ParkingSession subscriptionOpenSession(TestData data) {
        ParkingSession parkingSession = baseOpenSession(data, Instant.now().minusSeconds(6 * 60 * 60));
        parkingSession.setCustomerId(data.customerId());
        parkingSession.setCustomerVehicleId(data.customerVehicleId());
        return parkingSession;
    }

    private ParkingSession baseOpenSession(TestData data, Instant checkInTime) {
        ParkingSession parkingSession = new ParkingSession();
        parkingSession.setParkingSessionId(data.parkingSessionId());
        parkingSession.setCardId(data.cardId());
        parkingSession.setVehicleTypeId(data.vehicleTypeId());
        parkingSession.setZoneId(data.zoneId());
        parkingSession.setLicensePlateIn("51A-12345");
        parkingSession.setCheckInTime(checkInTime);
        parkingSession.setStatus(ParkingSessionStatus.OPEN);
        return parkingSession;
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

    private PriceRule priceRule(UUID vehicleTypeId, BigDecimal basePrice, LocalTime timeFrom, LocalTime timeTo) {
        PriceRule priceRule = new PriceRule();
        priceRule.setPriceRuleId(UUID.randomUUID());
        priceRule.setPricePlanId(UUID.randomUUID());
        priceRule.setVehicleTypeId(vehicleTypeId);
        priceRule.setTicketTypeId(UUID.randomUUID());
        priceRule.setRuleName("Parking price");
        priceRule.setBasePrice(basePrice);
        priceRule.setTimeFrom(timeFrom);
        priceRule.setTimeTo(timeTo);
        priceRule.setIsActive(Boolean.TRUE);
        return priceRule;
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

    private Zone zone(UUID zoneId, UUID parkingLotId, UUID vehicleTypeId, ZoneStatus status) {
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(parkingLotId);
        zone.setVehicleTypeId(vehicleTypeId);
        zone.setCapacity(100);
        zone.setStatus(status);
        return zone;
    }

    private ParkingLot parkingLot(UUID parkingLotId, ParkingLotStatus status) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(parkingLotId);
        parkingLot.setStatus(status);
        return parkingLot;
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
                "pe/2026/06/29/event/pv-checkout-plate.jpg",
                "plate.jpg",
                "image/jpeg",
                3,
                "checksum"
        );
    }

    private StoredFile storedPersonFile() {
        return new StoredFile(
                "pe/2026/06/29/event/pv-checkout-person.jpg",
                "person.jpg",
                "image/jpeg",
                3,
                "checksum-person"
        );
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
            UUID parkingSessionId,
            UUID customerId,
            UUID customerVehicleId
    ) {
        UUID actorAccountId() {
            return UUID.randomUUID();
        }
    }
}
