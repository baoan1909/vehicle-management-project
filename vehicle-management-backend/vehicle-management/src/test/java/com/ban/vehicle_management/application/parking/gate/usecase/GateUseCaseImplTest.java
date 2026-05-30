package com.ban.vehicle_management.application.parking.gate.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GateUseCaseImplTest {

    @Mock
    private GatePortOut gatePortOut;

    @InjectMocks
    private GateUseCaseImpl gateUseCase;

    @Test
    void shouldCreateGateWhenValid() {
        UUID zoneId = UUID.randomUUID();
        Gate request = validGate(zoneId);

        when(gatePortOut.existsActiveZoneById(zoneId)).thenReturn(true);
        when(gatePortOut.existsByZoneIdAndCode(zoneId, "MOTO-GATE-01")).thenReturn(false);
        when(gatePortOut.save(any(Gate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Gate createdGate = gateUseCase.createGate(request);

        assertNotNull(createdGate.getGateId());
        assertEquals("MOTO-GATE-01", createdGate.getCode());
        assertEquals(GateStatus.ACTIVE, createdGate.getStatus());
    }

    @Test
    void shouldRejectCreateWhenZoneIsNotActive() {
        UUID zoneId = UUID.randomUUID();
        Gate request = validGate(zoneId);

        when(gatePortOut.existsActiveZoneById(zoneId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> gateUseCase.createGate(request));
        verify(gatePortOut, never()).save(any(Gate.class));
    }

    @Test
    void shouldRejectCreateWhenCodeAlreadyExistsInZone() {
        UUID zoneId = UUID.randomUUID();
        Gate request = validGate(zoneId);

        when(gatePortOut.existsActiveZoneById(zoneId)).thenReturn(true);
        when(gatePortOut.existsByZoneIdAndCode(zoneId, "MOTO-GATE-01")).thenReturn(true);

        assertThrows(ConflictException.class, () -> gateUseCase.createGate(request));
        verify(gatePortOut, never()).save(any(Gate.class));
    }

    @Test
    void shouldReturnGateById() {
        UUID gateId = UUID.randomUUID();
        Gate existingGate = validGate(UUID.randomUUID());
        existingGate.setGateId(gateId);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));

        Gate result = gateUseCase.getGateById(gateId);

        assertEquals(gateId, result.getGateId());
    }

    @Test
    void shouldThrowWhenGateDoesNotExist() {
        UUID gateId = UUID.randomUUID();

        when(gatePortOut.findById(gateId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> gateUseCase.getGateById(gateId));
    }

    @Test
    void shouldReturnFilteredGatesWithTrimmedKeyword() {
        UUID zoneId = UUID.randomUUID();

        when(gatePortOut.findAll(zoneId, GateStatus.ACTIVE, "MOTO"))
                .thenReturn(List.of(new Gate(), new Gate()));

        List<Gate> gates = gateUseCase.getGates(zoneId, GateStatus.ACTIVE, " MOTO ");

        assertEquals(2, gates.size());
        verify(gatePortOut).findAll(zoneId, GateStatus.ACTIVE, "MOTO");
    }

    @Test
    void shouldUpdateGateWhenValid() {
        UUID gateId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Gate existingGate = validGate(zoneId);
        existingGate.setGateId(gateId);
        existingGate.setStatus(GateStatus.MAINTENANCE);

        Gate request = new Gate();
        request.setCode(" moto-gate-02 ");
        request.setName(" Cong xe may so 2 ");

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));
        when(gatePortOut.existsByZoneIdAndCodeAndGateIdNot(zoneId, "MOTO-GATE-02", gateId)).thenReturn(false);
        when(gatePortOut.save(any(Gate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Gate updatedGate = gateUseCase.updateGate(gateId, request);

        assertEquals("MOTO-GATE-02", updatedGate.getCode());
        assertEquals("Cong xe may so 2", updatedGate.getName());
        assertEquals(zoneId, updatedGate.getZoneId());
        assertEquals(GateStatus.MAINTENANCE, updatedGate.getStatus());
    }

    @Test
    void shouldRejectUpdateWhenCodeAlreadyExistsInZone() {
        UUID gateId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Gate existingGate = validGate(zoneId);
        existingGate.setGateId(gateId);

        Gate request = new Gate();
        request.setCode("MOTO-GATE-02");
        request.setName("Cong xe may so 2");

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));
        when(gatePortOut.existsByZoneIdAndCodeAndGateIdNot(zoneId, "MOTO-GATE-02", gateId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> gateUseCase.updateGate(gateId, request));
        verify(gatePortOut, never()).save(any(Gate.class));
    }

    @Test
    void shouldCloseGateOnDelete() {
        UUID gateId = UUID.randomUUID();
        Gate existingGate = validGate(UUID.randomUUID());
        existingGate.setGateId(gateId);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));

        gateUseCase.deleteGate(gateId);

        assertEquals(GateStatus.CLOSED, existingGate.getStatus());
        verify(gatePortOut).save(existingGate);
    }

    @Test
    void shouldDoNothingWhenDeletingClosedGate() {
        UUID gateId = UUID.randomUUID();
        Gate existingGate = validGate(UUID.randomUUID());
        existingGate.setGateId(gateId);
        existingGate.setStatus(GateStatus.CLOSED);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));

        gateUseCase.deleteGate(gateId);

        verify(gatePortOut, never()).save(any(Gate.class));
    }

    @Test
    void shouldRejectDeleteWhenGateHasActiveLanes() {
        UUID gateId = UUID.randomUUID();
        Gate existingGate = validGate(UUID.randomUUID());
        existingGate.setGateId(gateId);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));
        when(gatePortOut.hasActiveLanes(gateId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> gateUseCase.deleteGate(gateId));
        verify(gatePortOut, never()).save(any(Gate.class));
    }

    @Test
    void shouldActivateGateWhenZoneIsActive() {
        UUID gateId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Gate existingGate = validGate(zoneId);
        existingGate.setGateId(gateId);
        existingGate.setStatus(GateStatus.CLOSED);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));
        when(gatePortOut.existsActiveZoneById(zoneId)).thenReturn(true);
        when(gatePortOut.save(any(Gate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Gate activatedGate = gateUseCase.activateGate(gateId);

        assertEquals(GateStatus.ACTIVE, activatedGate.getStatus());
    }

    @Test
    void shouldRejectActivateWhenZoneIsNotActive() {
        UUID gateId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        Gate existingGate = validGate(zoneId);
        existingGate.setGateId(gateId);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));
        when(gatePortOut.existsActiveZoneById(zoneId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> gateUseCase.activateGate(gateId));
        verify(gatePortOut, never()).save(any(Gate.class));
    }

    @Test
    void shouldMarkGateMaintenance() {
        UUID gateId = UUID.randomUUID();
        Gate existingGate = validGate(UUID.randomUUID());
        existingGate.setGateId(gateId);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));
        when(gatePortOut.save(any(Gate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Gate gate = gateUseCase.markGateMaintenance(gateId);

        assertEquals(GateStatus.MAINTENANCE, gate.getStatus());
    }

    @Test
    void shouldCloseGate() {
        UUID gateId = UUID.randomUUID();
        Gate existingGate = validGate(UUID.randomUUID());
        existingGate.setGateId(gateId);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));
        when(gatePortOut.save(any(Gate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Gate gate = gateUseCase.closeGate(gateId);

        assertEquals(GateStatus.CLOSED, gate.getStatus());
    }

    @Test
    void shouldRejectCloseWhenGateHasActiveLanes() {
        UUID gateId = UUID.randomUUID();
        Gate existingGate = validGate(UUID.randomUUID());
        existingGate.setGateId(gateId);

        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(existingGate));
        when(gatePortOut.hasActiveLanes(gateId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> gateUseCase.closeGate(gateId));
        verify(gatePortOut, never()).save(any(Gate.class));
    }

    private Gate validGate(UUID zoneId) {
        Gate gate = new Gate();
        gate.setZoneId(zoneId);
        gate.setCode("MOTO-GATE-01");
        gate.setName("Cong xe may so 1");
        gate.setStatus(GateStatus.ACTIVE);
        return gate;
    }
}
