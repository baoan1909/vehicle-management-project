package com.ban.vehicle_management.application.parking.lanes.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.application.parking.lane.usecase.LaneStatusNotificationDispatcher;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LaneStatusNotificationDispatcherTest {

    @Mock private OrganizationPortOut organizationPortOut;
    @Mock private RequiresNewNotificationSender notificationSender;

    private LaneStatusNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new LaneStatusNotificationDispatcher(organizationPortOut, notificationSender);
    }

    @Test
    void sendsOnlyToScopedEligibleOperationalAccounts() {
        UUID parkingLotId = UUID.randomUUID();
        UUID actorAccountId = UUID.randomUUID();
        UUID employeeAccountId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        when(organizationPortOut.findActiveOperationalAccountIdsByParkingLotId(parkingLotId))
                .thenReturn(Set.of(actorAccountId, employeeAccountId));
        when(notificationSender.sendBroadcast(any())).thenReturn(List.of(new Notification()));

        dispatcher.dispatch(
                parkingLotId,
                actorAccountId,
                laneId,
                "Lane Làn vào đã được kích hoạt thành công.",
                NotificationType.LANE_ACTIVATED,
                "Lane kích hoạt",
                "Lane Làn vào đã chuyển sang trạng thái hoạt động."
        );

        ArgumentCaptor<BroadcastNotificationCommand> captor =
                ArgumentCaptor.forClass(BroadcastNotificationCommand.class);
        verify(notificationSender).sendBroadcast(captor.capture());
        BroadcastNotificationCommand command = captor.getValue();
        assertEquals(Set.of(actorAccountId, employeeAccountId), command.accountIds());
        assertEquals(NotificationType.LANE_ACTIVATED, command.notificationType());
        assertTrue(command.recipientCriteria().requireBusinessAccess());
        assertTrue(command.recipientCriteria().excludedAccountIds().contains(actorAccountId));
        assertTrue(command.recipientCriteria().requiredAnyPermissionCodes()
                .contains("PARKING_SPACE_CONFIGURE_ALL"));
        verify(notificationSender, never()).send(any(SendNotificationCommand.class));
    }

    @Test
    void notifiesActorWhenNoScopedRecipientExists() {
        UUID parkingLotId = UUID.randomUUID();
        UUID actorAccountId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        when(organizationPortOut.findActiveOperationalAccountIdsByParkingLotId(parkingLotId))
                .thenReturn(Set.of());

        dispatcher.dispatch(
                parkingLotId,
                actorAccountId,
                laneId,
                "Lane Làn vào đã được kích hoạt thành công.",
                NotificationType.LANE_ACTIVATED,
                "Lane kích hoạt",
                "Lane Làn vào đã chuyển sang trạng thái hoạt động."
        );

        ArgumentCaptor<SendNotificationCommand> captor =
                ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(notificationSender).send(captor.capture());
        assertEquals(actorAccountId, captor.getValue().accountId());
        assertEquals(NotificationType.SYSTEM_NOTICE, captor.getValue().notificationType());
        assertEquals(
                "Lane Làn vào đã được kích hoạt thành công. "
                        + "Hiện không có nhân sự vận hành đủ điều kiện nhận thông báo.",
                captor.getValue().message()
        );
        verify(notificationSender, never()).sendBroadcast(any(BroadcastNotificationCommand.class));
    }

    @Test
    void treatsPermissionFilteringToZeroAsNoRecipient() {
        UUID parkingLotId = UUID.randomUUID();
        UUID actorAccountId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        when(organizationPortOut.findActiveOperationalAccountIdsByParkingLotId(parkingLotId))
                .thenReturn(Set.of(UUID.randomUUID()));
        when(notificationSender.sendBroadcast(any())).thenReturn(List.of());

        dispatcher.dispatch(
                parkingLotId,
                actorAccountId,
                laneId,
                "Lane Làn vào đã được kích hoạt thành công.",
                NotificationType.LANE_ACTIVATED,
                "Lane kích hoạt",
                "Lane Làn vào đã chuyển sang trạng thái hoạt động."
        );

        verify(notificationSender).send(any(SendNotificationCommand.class));
    }

    @Test
    void notificationDatabaseOrRealtimeFailureNeverEscapesDispatcher() {
        UUID parkingLotId = UUID.randomUUID();
        UUID actorAccountId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        when(organizationPortOut.findActiveOperationalAccountIdsByParkingLotId(parkingLotId))
                .thenReturn(Set.of(UUID.randomUUID()));
        when(notificationSender.sendBroadcast(any())).thenThrow(new IllegalStateException("notification failure"));
        org.mockito.Mockito.doThrow(new IllegalStateException("fallback failure"))
                .when(notificationSender).send(any(SendNotificationCommand.class));

        assertDoesNotThrow(() -> dispatcher.dispatch(
                parkingLotId,
                actorAccountId,
                laneId,
                "Lane Làn vào đã được kích hoạt thành công.",
                NotificationType.LANE_ACTIVATED,
                "Lane kích hoạt",
                "Lane Làn vào đã chuyển sang trạng thái hoạt động."
        ));
    }
}
