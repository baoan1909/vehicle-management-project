package com.ban.vehicle_management.application.parking.lanes.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.application.parking.gate.port.out.GatePortOut;
import com.ban.vehicle_management.application.parking.lane.port.out.LanePortOut;
import com.ban.vehicle_management.application.parking.lane.usecase.LaneStatusNotificationDispatcher;
import com.ban.vehicle_management.application.parking.lane.usecase.LaneUsecaseImpl;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LaneStatusAfterCommitFlowTest {

    @Test
    void notificationFailureDoesNotUndoActivatedLaneAndAudit() {
        LanePortOut lanePortOut = mock(LanePortOut.class);
        GatePortOut gatePortOut = mock(GatePortOut.class);
        ZonePortOut zonePortOut = mock(ZonePortOut.class);
        ParkingLotPortOut parkingLotPortOut = mock(ParkingLotPortOut.class);
        OrganizationAccessGuard accessGuard = mock(OrganizationAccessGuard.class);
        AuditLogPortOut auditLogPortOut = mock(AuditLogPortOut.class);
        CurrentAccountPortIn currentAccountPortIn = mock(CurrentAccountPortIn.class);
        OrganizationPortOut organizationPortOut = mock(OrganizationPortOut.class);
        RequiresNewNotificationSender notificationSender = mock(RequiresNewNotificationSender.class);
        LaneStatusNotificationDispatcher dispatcher =
                new LaneStatusNotificationDispatcher(organizationPortOut, notificationSender);

        UUID laneId = UUID.randomUUID();
        UUID gateId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID actorAccountId = UUID.randomUUID();

        Lane lane = new Lane();
        lane.setLaneId(laneId);
        lane.setGateId(gateId);
        lane.setCode("LANE-IN-01");
        lane.setName("Làn vào");
        lane.setDirection(LaneDirection.IN);
        lane.setStatus(LaneStatus.CLOSED);
        Gate gate = mock(Gate.class);
        Zone zone = mock(Zone.class);
        ParkingLot parkingLot = mock(ParkingLot.class);

        when(lanePortOut.findById(laneId)).thenReturn(Optional.of(lane));
        when(lanePortOut.existsOperationalGateById(gateId)).thenReturn(true);
        when(lanePortOut.save(any(Lane.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gatePortOut.findById(gateId)).thenReturn(Optional.of(gate));
        when(gate.getZoneId()).thenReturn(zoneId);
        when(zonePortOut.findById(zoneId)).thenReturn(Optional.of(zone));
        when(zone.getParkingLotId()).thenReturn(parkingLotId);
        when(parkingLotPortOut.findById(parkingLotId)).thenReturn(Optional.of(parkingLot));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actorAccountId);
        when(organizationPortOut.findActiveOperationalAccountIdsByParkingLotId(parkingLotId))
                .thenReturn(Set.of(UUID.randomUUID()));
        when(notificationSender.sendBroadcast(any(BroadcastNotificationCommand.class)))
                .thenThrow(new IllegalStateException("notification database unavailable"));
        org.mockito.Mockito.doThrow(new IllegalStateException("fallback database unavailable"))
                .when(notificationSender).send(any(SendNotificationCommand.class));

        LaneUsecaseImpl useCase = new LaneUsecaseImpl(
                lanePortOut,
                gatePortOut,
                zonePortOut,
                parkingLotPortOut,
                accessGuard,
                auditLogPortOut,
                currentAccountPortIn,
                dispatcher
        );

        Lane result = assertDoesNotThrow(() -> useCase.activateLane(laneId));

        assertEquals(LaneStatus.ACTIVE, result.getStatus());
        verify(lanePortOut).save(lane);
        verify(auditLogPortOut).save(any(AuditLog.class));
    }
}
