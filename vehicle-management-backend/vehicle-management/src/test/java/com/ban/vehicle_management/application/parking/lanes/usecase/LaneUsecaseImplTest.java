package com.ban.vehicle_management.application.parking.lanes.usecase;

import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.lane.usecase.LaneUsecaseImpl;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LaneUsecaseImplTest {

    @Mock
    private LanePortOut lanePortOut;

    @InjectMocks
    private LaneUsecaseImpl laneUsecase;

    @Test
    void shouldCreateLaneWhenGateIsOperational(){
        UUID gateId = UUID.randomUUID();
        Lane request = validLane(gateId, LaneDirection.IN);

        when(lanePortOut.existsOperationalGateById(gateId)).thenReturn(true);
        when(lanePortOut.existsByGateIdAndCode(gateId, "LANE-IN-01")).thenReturn(false);
        when(lanePortOut.save(any(Lane.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Lane createdLane = laneUsecase.createLane((request));

        assertNotNull(createdLane.getLaneId());
        assertEquals(gateId, createdLane.getGateId());
        assertEquals("LANE-IN-01", createdLane.getCode());
        assertEquals(LaneStatus.ACTIVE, createdLane.getStatus());
    }

    @Test
    void shouldRejectCreateWhenGateIsNotOperational(){
        UUID gateId = UUID.randomUUID();
        Lane request = validLane(gateId,LaneDirection.IN);

        when(lanePortOut.existsOperationalGateById(gateId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> laneUsecase.createLane(request));
        verify(lanePortOut, never()).save(any(Lane.class));
    }

    @Test
    void shouldRejectCreateWhenCodeAlreadyExistsInGate(){
        UUID gateId = UUID.randomUUID();
        Lane request = validLane(gateId, LaneDirection.IN);

        when(lanePortOut.existsOperationalGateById(gateId)).thenReturn(true);
        when(lanePortOut.existsByGateIdAndCode(gateId, "LANE-IN-01")).thenReturn(true);

        assertThrows(ConflictException.class, () -> laneUsecase.createLane(request));
        verify(lanePortOut, never()).save(any(Lane.class));
    }

    @Test
    void shouldReturnLaneById() {
        UUID laneId = UUID.randomUUID();
        Lane lane = validLane(UUID.randomUUID(), LaneDirection.IN);
        lane.setLaneId(laneId);

        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(lane));

        Lane result = laneUsecase.getLaneById(laneId);

        assertEquals(laneId, result.getLaneId());
    }

    @Test
    void shouldThrowWhenLaneDoesNotExist() {
        UUID laneId = UUID.randomUUID();

        when(lanePortOut.findById(laneId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> laneUsecase.getLaneById(laneId));
    }

    @Test
    void shouldReturnFilteredLanesWithTrimmedKeyword() {
        UUID gateId = UUID.randomUUID();

        when(lanePortOut.findAll(gateId, LaneDirection.OUT, LaneStatus.ACTIVE, "OUT"))
                .thenReturn(List.of(new Lane(), new Lane()));

        List<Lane> lanes = laneUsecase.getLanes(gateId, LaneDirection.OUT, LaneStatus.ACTIVE, " OUT ");

        assertEquals(2, lanes.size());
        verify(lanePortOut).findAll(gateId, LaneDirection.OUT, LaneStatus.ACTIVE, "OUT");
    }

    @Test
    void shouldUpdateLaneWhenValid() {
        UUID gateId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();

        Lane existingLane = validLane(gateId, LaneDirection.IN);
        existingLane.setLaneId(laneId);

        Lane request = new Lane();
        request.setCode(" lane-out-01 ");
        request.setName(" Lane out 01 ");
        request.setDirection(LaneDirection.OUT);

        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(existingLane));
        when(lanePortOut.existsByGateIdAndCodeAndLaneIdNot(gateId, "LANE-OUT-01", laneId)).thenReturn(false);
        when(lanePortOut.save(any(Lane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lane updatedLane = laneUsecase.updateLane(laneId, request);

        assertEquals("LANE-OUT-01", updatedLane.getCode());
        assertEquals("Lane out 01", updatedLane.getName());
        assertEquals(LaneDirection.OUT, updatedLane.getDirection());
    }

    @Test
    void shouldRejectUpdateWhenDisablingLastActiveOutLaneWithOpenSessions() {
        UUID gateId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();

        Lane existingLane = validLane(gateId, LaneDirection.OUT);
        existingLane.setLaneId(laneId);
        existingLane.setStatus(LaneStatus.ACTIVE);

        Lane request = new Lane();
        request.setCode("LANE-IN-01");
        request.setName("Lane in 01");
        request.setDirection(LaneDirection.IN);

        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(existingLane));
        when(lanePortOut.findZoneIdByGateId(gateId)).thenReturn(Optional.of(zoneId));
        when(lanePortOut.hasOpenSessionsInZone(zoneId)).thenReturn(true);
        when(lanePortOut.hasOtherActiveOutLaneInZone(zoneId, laneId)).thenReturn(false);

        assertThrows(ConflictException.class, () -> laneUsecase.updateLane(laneId, request));
        verify(lanePortOut, never()).save(any(Lane.class));
    }

    @Test
    void shouldMarkLaneMaintenanceWhenAllowed() {
        UUID laneId = UUID.randomUUID();

        Lane existingLane = validLane(UUID.randomUUID(), LaneDirection.IN);
        existingLane.setLaneId(laneId);

        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(existingLane));
        when(lanePortOut.save(any(Lane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lane lane = laneUsecase.markLaneMaintenance(laneId);

        assertEquals(LaneStatus.MAINTENANCE, lane.getStatus());
    }

    @Test
    void shouldForceMaintenanceWithoutCheckingLastOutRule() {
        UUID laneId = UUID.randomUUID();

        Lane existingLane = validLane(UUID.randomUUID(), LaneDirection.OUT);
        existingLane.setLaneId(laneId);

        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(existingLane));
        when(lanePortOut.save(any(Lane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lane lane = laneUsecase.forceLaneMaintenance(laneId);

        assertEquals(LaneStatus.MAINTENANCE, lane.getStatus());
        verify(lanePortOut, never()).hasOpenSessionsInZone(any(UUID.class));
    }

    @Test
    void shouldActivateLaneWhenGateIsOperational() {
        UUID gateId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();

        Lane existingLane = validLane(gateId, LaneDirection.IN);
        existingLane.setLaneId(laneId);
        existingLane.setStatus(LaneStatus.CLOSED);

        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(existingLane));
        when(lanePortOut.existsOperationalGateById(gateId)).thenReturn(true);
        when(lanePortOut.save(any(Lane.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lane lane = laneUsecase.activateLane(laneId);

        assertEquals(LaneStatus.ACTIVE, lane.getStatus());
    }

    @Test
    void shouldCloseLaneOnDelete() {
        UUID laneId = UUID.randomUUID();

        Lane existingLane = validLane(UUID.randomUUID(), LaneDirection.IN);
        existingLane.setLaneId(laneId);

        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(existingLane));

        laneUsecase.deleteLane(laneId);

        assertEquals(LaneStatus.CLOSED, existingLane.getStatus());
        verify(lanePortOut).save(existingLane);
    }


    private Lane validLane(UUID gateId, LaneDirection direction){
        Lane lane = new Lane();
        lane.setGateId(gateId);
        lane.setCode("LANE-IN-01");
        lane.setName("lane in 01");
        lane.setDirection(direction);
        lane.setStatus(LaneStatus.ACTIVE);
        return lane;
    }
}
