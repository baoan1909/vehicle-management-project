package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.out.ParkingSpaceAllocationPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceAllocationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
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
class ParkingCheckOutSpaceReleaseTest {

    @Mock private ParkingSessionAccessGuard parkingSessionAccessGuard;
    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private CardPortOut cardPortOut;
    @Mock private LanePortOut lanePortOut;
    @Mock private GatePortOut gatePortOut;
    @Mock private ZonePortOut zonePortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ParkingSessionPortOut parkingSessionPortOut;
    @Mock private ParkingEventPortOut parkingEventPortOut;
    @Mock private PriceRulePortOut priceRulePortOut;
    @Mock private InvoicePortOut invoicePortOut;
    @Mock private FileStoragePort fileStoragePort;
    @Mock private FileAccessPort fileAccessPort;
    @Mock private ParkingSpacePortOut parkingSpacePortOut;
    @Mock private ParkingSpaceAllocationPortOut allocationPortOut;

    private ParkingCheckOutUseCaseImpl useCase;

    private final UUID cardId = UUID.randomUUID();
    private final UUID laneId = UUID.randomUUID();
    private final UUID gateId = UUID.randomUUID();
    private final UUID zoneId = UUID.randomUUID();
    private final UUID lotId = UUID.randomUUID();
    private final UUID spaceId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ParkingCheckOutUseCaseImpl(
                parkingSessionAccessGuard, currentAccountPortIn, cardPortOut, lanePortOut, gatePortOut,
                zonePortOut, parkingLotPortOut, parkingSessionPortOut, parkingEventPortOut,
                priceRulePortOut, invoicePortOut, Mappers.getMapper(ParkingCheckOutMapper.class),
                fileStoragePort, fileAccessPort, parkingSpacePortOut, allocationPortOut, null);
    }

    @Test
    void shouldReleaseSpaceToAvailableWhenNoAllocationRemains() {
        mockTopology();
        ParkingSession session = openSubscriptionSession();
        ParkingSpace space = occupiedSpace();
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(card()));
        when(parkingSessionPortOut.findOpenByCardId(cardId)).thenReturn(Optional.of(session));
        when(parkingSessionPortOut.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(storedFile(), storedFile());
        when(parkingEventPortOut.save(any(ParkingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(space));
        when(allocationPortOut.findActiveAllocationsForSpaceOnDate(any(UUID.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(parkingSpacePortOut.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckOutResult result = useCase.checkOut(command());

        assertEquals(ParkingSessionStatus.CLOSED, result.parkingSession().getStatus());
        assertEquals(ParkingSpaceStatus.AVAILABLE, space.getStatus());
        verify(parkingSpacePortOut).save(space);
    }

    @Test
    void shouldReleaseSpaceToReservedWhenAllocationRemains() {
        mockTopology();
        ParkingSession session = openSubscriptionSession();
        ParkingSpace space = occupiedSpace();
        when(cardPortOut.findByUidForUpdate("UID-001")).thenReturn(Optional.of(card()));
        when(parkingSessionPortOut.findOpenByCardId(cardId)).thenReturn(Optional.of(session));
        when(parkingSessionPortOut.save(any(ParkingSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileStoragePort.store(any(StoreFileCommand.class)))
                .thenReturn(storedFile(), storedFile());
        when(parkingEventPortOut.save(any(ParkingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(parkingSpacePortOut.findById(spaceId)).thenReturn(Optional.of(space));
        ParkingSpaceAllocation remaining = new ParkingSpaceAllocation();
        remaining.setAllocationStatus(ParkingSpaceAllocationStatus.ACTIVE);
        when(allocationPortOut.findActiveAllocationsForSpaceOnDate(any(UUID.class), any(LocalDate.class)))
                .thenReturn(List.of(remaining));
        when(parkingSpacePortOut.save(any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.checkOut(command());

        assertEquals(ParkingSpaceStatus.RESERVED, space.getStatus());
        verify(parkingSpacePortOut).save(space);
    }

    private CheckOutCommand command() {
        return new CheckOutCommand(laneId, "UID-001", "51A-12345",
                new MockMultipartFile("plate", "plate.jpg", "image/jpeg", new byte[] {1}),
                new MockMultipartFile("person", "person.jpg", "image/jpeg", new byte[] {2}),
                null);
    }

    private void mockTopology() {
        Lane lane = new Lane();
        lane.setLaneId(laneId);
        lane.setGateId(gateId);
        lane.setDirection(LaneDirection.OUT);
        lane.setStatus(LaneStatus.ACTIVE);
        Gate gate = new Gate();
        gate.setGateId(gateId);
        gate.setZoneId(zoneId);
        gate.setStatus(GateStatus.ACTIVE);
        Zone zone = new Zone();
        zone.setZoneId(zoneId);
        zone.setParkingLotId(lotId);
        zone.setVehicleTypeId(UUID.randomUUID());
        zone.setCapacity(100);
        zone.setStatus(ZoneStatus.ACTIVE);
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setStatus(ParkingLotStatus.ACTIVE);
        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(lane));
        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(gate));
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));
    }

    private Card card() {
        Card card = new Card();
        card.setCardId(cardId);
        card.setCardNumber("C001");
        card.setUid("UID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setStatus(CardStatus.IN_USE);
        return card;
    }

    private ParkingSession openSubscriptionSession() {
        ParkingSession session = new ParkingSession();
        session.setParkingSessionId(sessionId);
        session.setCardId(cardId);
        session.setZoneId(zoneId);
        session.setParkingLotId(lotId);
        session.setParkingSpaceId(spaceId);
        session.setCustomerId(UUID.randomUUID());
        session.setCustomerVehicleId(UUID.randomUUID());
        session.setVehicleTypeId(UUID.randomUUID());
        session.setLicensePlateIn("51A12345");
        session.setCheckInTime(java.time.Instant.now().minusSeconds(3600));
        session.setStatus(ParkingSessionStatus.OPEN);
        return session;
    }

    private ParkingSpace occupiedSpace() {
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(spaceId);
        space.setZoneId(zoneId);
        space.setCode("A-01");
        space.setLifecycleStatus("ACTIVE");
        space.setStatus(ParkingSpaceStatus.OCCUPIED);
        space.setStatusSource(ParkingSpaceStatusSource.AUTOMATIC);
        space.setVersion(0L);
        space.setManualOccupancyType(ManualOccupancyType.REGISTERED);
        return space;
    }

    private StoredFile storedFile() {
        return new StoredFile("pe/key/file.jpg", "file.jpg", "image/jpeg", 3, "checksum");
    }
}
