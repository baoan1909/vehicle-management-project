package com.ban.vehicle_management.application.parking.parkinglot.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingLotUseCaseImplTest {

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private OrganizationAccessGuard organizationAccessGuard;

    @InjectMocks
    private ParkingLotUseCaseImpl parkingLotUseCase;

    @BeforeEach
    void setUp() {
        lenient().when(organizationAccessGuard.resolveOrganizationIdForParkingLotCreation(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenReturn(ParkingLotAccessScope.unrestrictedScope());
    }

    @Test
    void shouldCreateParkingLotWhenValid() {
        ParkingLot request = validParkingLot();

        when(parkingLotPortOut.existsByOrganizationIdAndCode(request.getOrganizationId(), "HCMUTE")).thenReturn(false);
        when(parkingLotPortOut.save(any(ParkingLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingLot createdParkingLot = parkingLotUseCase.createParkingLot(request);

        assertNotNull(createdParkingLot.getParkingLotId());
        assertEquals("HCMUTE", createdParkingLot.getCode());
        assertEquals(ParkingLotStatus.SETUP, createdParkingLot.getStatus());
    }

    @Test
    void shouldRejectCreateWhenCodeAlreadyExists() {
        ParkingLot request = validParkingLot();

        when(parkingLotPortOut.existsByOrganizationIdAndCode(request.getOrganizationId(), "HCMUTE")).thenReturn(true);

        assertThrows(ConflictException.class, () -> parkingLotUseCase.createParkingLot(request));
        verify(parkingLotPortOut, never()).save(any(ParkingLot.class));
    }

    @Test
    void shouldReturnParkingLotById() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));

        ParkingLot result = parkingLotUseCase.getParkingLotById(parkingLotId);

        assertEquals(parkingLotId, result.getParkingLotId());
    }

    @Test
    void shouldThrowWhenParkingLotDoesNotExist() {
        UUID parkingLotId = UUID.randomUUID();

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> parkingLotUseCase.getParkingLotById(parkingLotId));
    }

    @Test
    void shouldReturnFilteredParkingLotsWithTrimmedKeyword() {
        when(parkingLotPortOut.findAll(ParkingLotStatus.ACTIVE, "HCMUTE", null, null))
                .thenReturn(List.of(new ParkingLot(), new ParkingLot()));

        List<ParkingLot> parkingLots = parkingLotUseCase.getParkingLots(
                ParkingLotStatus.ACTIVE,
                " HCMUTE "
        );

        assertEquals(2, parkingLots.size());
        verify(parkingLotPortOut).findAll(ParkingLotStatus.ACTIVE, "HCMUTE", null, null);
    }

    @Test
    void shouldReturnNoParkingLotsWhenManagerHasNoAssignedScope() {
        when(organizationAccessGuard.resolveParkingLotAccessScope())
                .thenReturn(new ParkingLotAccessScope(false, java.util.Set.of(), java.util.Set.of()));
        when(parkingLotPortOut.findAll(ParkingLotStatus.ACTIVE, null, null, java.util.Set.of()))
                .thenReturn(List.of());

        List<ParkingLot> parkingLots = parkingLotUseCase.getParkingLots(ParkingLotStatus.ACTIVE, null);

        assertEquals(List.of(), parkingLots);
        verify(parkingLotPortOut).findAll(ParkingLotStatus.ACTIVE, null, null, java.util.Set.of());
    }

    @Test
    void shouldUpdateParkingLotWhenValid() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);
        existingParkingLot.setStatus(ParkingLotStatus.MAINTENANCE);

        ParkingLot request = new ParkingLot();
        request.setCode(" hcmute-main ");
        request.setName(" Bai xe HCMUTE Main ");
        request.setAddress(" Dia chi moi ");
        request.setTotalCapacity(1200);

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));
        when(parkingLotPortOut.existsByOrganizationIdAndCodeAndParkingLotIdNot(
                existingParkingLot.getOrganizationId(),
                "HCMUTE-MAIN",
                parkingLotId
        )).thenReturn(false);
        when(parkingLotPortOut.save(any(ParkingLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingLot updatedParkingLot = parkingLotUseCase.updateParkingLot(parkingLotId, request);

        assertEquals("HCMUTE-MAIN", updatedParkingLot.getCode());
        assertEquals("Bai xe HCMUTE Main", updatedParkingLot.getName());
        assertEquals("Dia chi moi", updatedParkingLot.getAddress());
        assertEquals(1200, updatedParkingLot.getTotalCapacity());
        assertEquals(ParkingLotStatus.MAINTENANCE, updatedParkingLot.getStatus());
    }

    @Test
    void shouldRejectUpdateWhenCodeAlreadyExists() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);

        ParkingLot request = validParkingLot();
        request.setCode("OTHER");

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));
        when(parkingLotPortOut.existsByOrganizationIdAndCodeAndParkingLotIdNot(
                existingParkingLot.getOrganizationId(),
                "OTHER",
                parkingLotId
        )).thenReturn(true);

        assertThrows(ConflictException.class, () -> parkingLotUseCase.updateParkingLot(parkingLotId, request));
        verify(parkingLotPortOut, never()).save(any(ParkingLot.class));
    }

    @Test
    void shouldCloseParkingLotOnDelete() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);
        existingParkingLot.setStatus(ParkingLotStatus.ACTIVE);

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));
        when(parkingLotPortOut.save(any(ParkingLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        parkingLotUseCase.deleteParkingLot(parkingLotId);

        assertEquals(ParkingLotStatus.CLOSED, existingParkingLot.getStatus());
        verify(parkingLotPortOut).save(existingParkingLot);
    }

    @Test
    void shouldDoNothingWhenDeletingClosedParkingLot() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);
        existingParkingLot.setStatus(ParkingLotStatus.CLOSED);

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));

        parkingLotUseCase.deleteParkingLot(parkingLotId);

        verify(parkingLotPortOut, never()).save(any(ParkingLot.class));
    }

    @Test
    void shouldActivateParkingLot() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);
        existingParkingLot.setStatus(ParkingLotStatus.SETUP);
        existingParkingLot.setActivationRequestedAt(Instant.now());

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));
        when(parkingLotPortOut.isReadyForActivation(parkingLotId)).thenReturn(true);
        when(parkingLotPortOut.save(any(ParkingLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingLot activatedParkingLot = parkingLotUseCase.activateParkingLot(parkingLotId);

        assertEquals(ParkingLotStatus.ACTIVE, activatedParkingLot.getStatus());
    }

    @Test
    void shouldMarkParkingLotMaintenance() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));
        when(parkingLotPortOut.save(any(ParkingLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingLot parkingLot = parkingLotUseCase.markParkingLotMaintenance(parkingLotId);

        assertEquals(ParkingLotStatus.MAINTENANCE, parkingLot.getStatus());
    }

    @Test
    void shouldCloseParkingLot() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));
        when(parkingLotPortOut.save(any(ParkingLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingLot parkingLot = parkingLotUseCase.closeParkingLot(parkingLotId);

        assertEquals(ParkingLotStatus.CLOSED, parkingLot.getStatus());
    }

    private ParkingLot validParkingLot() {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setOrganizationId(UUID.randomUUID());
        parkingLot.setCode("HCMUTE");
        parkingLot.setName("Bai xe HCMUTE");
        parkingLot.setAddress("So 1 Vo Van Ngan");
        parkingLot.setTotalCapacity(1000);
        return parkingLot;
    }

    @Test
    void shouldRejectDeleteWhenParkingLotHasActiveZones() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);
        existingParkingLot.setStatus(ParkingLotStatus.ACTIVE);

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));
        when(parkingLotPortOut.hasActiveZones(parkingLotId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> parkingLotUseCase.deleteParkingLot(parkingLotId));
        verify(parkingLotPortOut, never()).save(any(ParkingLot.class));
    }

    @Test
    void shouldRejectCloseWhenParkingLotHasActiveZones() {
        UUID parkingLotId = UUID.randomUUID();
        ParkingLot existingParkingLot = validParkingLot();
        existingParkingLot.setParkingLotId(parkingLotId);

        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(existingParkingLot));
        when(parkingLotPortOut.hasActiveZones(parkingLotId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> parkingLotUseCase.closeParkingLot(parkingLotId));
        verify(parkingLotPortOut, never()).save(any(ParkingLot.class));
    }
}
