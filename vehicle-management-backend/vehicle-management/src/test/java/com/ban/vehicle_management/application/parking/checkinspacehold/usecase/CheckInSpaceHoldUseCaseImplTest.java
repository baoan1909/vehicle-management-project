package com.ban.vehicle_management.application.parking.checkinspacehold.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.checkinspacehold.config.CheckInSpaceHoldProperties;
import com.ban.vehicle_management.application.parking.checkinspacehold.model.command.CreateCheckInSpaceHoldCommand;
import com.ban.vehicle_management.application.parking.checkinspacehold.port.out.CheckInSpaceHoldPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.parking.checkinspacehold.model.CheckInSpaceHold;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckInSpaceHoldUseCaseImplTest {

    @Mock private ParkingSessionPortIn parkingSessionPortIn;
    @Mock private CheckInSpaceHoldPortOut holdPortOut;
    @Mock private ParkingSpacePortOut spacePortOut;
    @Mock private CardPortOut cardPortOut;
    @Mock private CurrentAccountPortIn currentAccountPortIn;

    private CheckInSpaceHoldUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        CheckInSpaceHoldProperties properties = new CheckInSpaceHoldProperties();
        properties.setDuration(Duration.ofSeconds(120));
        useCase = new CheckInSpaceHoldUseCaseImpl(
                parkingSessionPortIn, holdPortOut, spacePortOut, cardPortOut,
                currentAccountPortIn, properties);
    }

    @Test
    void shouldCreateServerTimedHoldForFirstCompatibleVisitorSpace() {
        UUID requestId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        Card card = new Card();
        card.setCardId(cardId);
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(spaceId);
        space.setZoneId(zoneId);
        space.setVehicleTypeId(vehicleTypeId);
        space.setCode("A-01");
        space.setStatus(ParkingSpaceStatus.AVAILABLE);
        CheckInPreviewResult preview = new CheckInPreviewResult(
                lotId, zoneId, vehicleTypeId, TrackingMode.SPACE, "VISITOR",
                null, null,
                3, 3, false, null,
                List.of(new CheckInPreviewResult.EligibleSpace(
                        spaceId, "A-01", ParkingSpaceStatus.AVAILABLE)));
        CreateCheckInSpaceHoldCommand command = new CreateCheckInSpaceHoldCommand(
                requestId, " RFID-001 ", laneId, vehicleTypeId, "59A-12345",
                null, null);
        Instant before = Instant.now();

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(operatorId);
        when(holdPortOut.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(parkingSessionPortIn.previewCheckIn(any())).thenReturn(preview);
        when(cardPortOut.findByUidForUpdate("RFID-001")).thenReturn(Optional.of(card));
        when(spacePortOut.findFirstVisitorAvailableForUpdate(eq(zoneId), eq(vehicleTypeId), any()))
                .thenReturn(Optional.of(space));
        when(holdPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(spacePortOut.findById(spaceId)).thenReturn(Optional.of(space));

        var result = useCase.create(command);

        Instant after = Instant.now();
        ArgumentCaptor<CheckInSpaceHold> holdCaptor = ArgumentCaptor.forClass(CheckInSpaceHold.class);
        verify(holdPortOut).save(holdCaptor.capture());
        CheckInSpaceHold saved = holdCaptor.getValue();
        verify(holdPortOut).lockTransactionKey("check-in-hold:request:" + requestId);
        verify(holdPortOut).lockTransactionKey("check-in-hold:plate:" + lotId + ":59A12345");
        assertTrue(result.created());
        assertEquals("A-01", result.parkingSpaceCode());
        assertEquals(CheckInSpaceHoldStatus.HELD, saved.getStatus());
        assertEquals(cardId, saved.getCardId());
        assertEquals(operatorId, saved.getOperatorAccountId());
        assertEquals("59A12345", saved.getLicensePlateNormalized());
        assertEquals(64, saved.getCredentialHash().length());
        assertNotEquals("RFID-001", saved.getCredentialHash());
        assertFalse(saved.getExpiresAt().isBefore(before.plusSeconds(120)));
        assertFalse(saved.getExpiresAt().isAfter(after.plusSeconds(120)));
    }

    @Test
    void shouldReturnOwnedHoldForRepeatedRequestIdWithoutAllocatingAgain() {
        UUID requestId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        CheckInSpaceHold existing = new CheckInSpaceHold();
        existing.setHoldId(UUID.randomUUID());
        existing.setRequestId(requestId);
        existing.setParkingLotId(UUID.randomUUID());
        existing.setZoneId(UUID.randomUUID());
        existing.setParkingSpaceId(spaceId);
        existing.setCustomerType("VISITOR");
        existing.setOperatorAccountId(operatorId);
        existing.setStatus(CheckInSpaceHoldStatus.HELD);
        existing.setExpiresAt(Instant.now().plusSeconds(60));
        ParkingSpace space = new ParkingSpace();
        space.setParkingSpaceId(spaceId);
        space.setCode("A-02");

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(operatorId);
        when(holdPortOut.findByRequestId(requestId)).thenReturn(Optional.of(existing));
        when(spacePortOut.findById(spaceId)).thenReturn(Optional.of(space));

        var result = useCase.create(new CreateCheckInSpaceHoldCommand(
                requestId, "RFID-002", UUID.randomUUID(), UUID.randomUUID(),
                "59A-67890", null, null));

        assertFalse(result.created());
        assertEquals(existing.getHoldId(), result.holdId());
        assertEquals("A-02", result.parkingSpaceCode());
        verifyNoInteractions(parkingSessionPortIn, cardPortOut);
    }
}
