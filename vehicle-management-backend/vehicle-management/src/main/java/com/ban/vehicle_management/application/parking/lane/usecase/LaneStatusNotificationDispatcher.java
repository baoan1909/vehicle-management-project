package com.ban.vehicle_management.application.parking.lane.usecase;

import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRecipientCriteria;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import io.micrometer.core.instrument.Metrics;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LaneStatusNotificationDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaneStatusNotificationDispatcher.class);
    private static final Set<String> REQUIRED_PERMISSION_CODES = Set.of(
            "PARKING_SPACE_CONFIGURE_ALL",
            "PARKING_SPACE_STATUS_UPDATE_ALL"
    );

    private final OrganizationPortOut organizationPortOut;
    private final RequiresNewNotificationSender notificationSender;

    public LaneStatusNotificationDispatcher(
            OrganizationPortOut organizationPortOut,
            RequiresNewNotificationSender notificationSender
    ) {
        this.organizationPortOut = organizationPortOut;
        this.notificationSender = notificationSender;
    }

    public void dispatch(
            UUID parkingLotId,
            UUID actorAccountId,
            UUID laneId,
            String successDescription,
            NotificationType notificationType,
            String title,
            String message
    ) {
        try {
            Set<UUID> scopedAccountIds = organizationPortOut
                    .findActiveOperationalAccountIdsByParkingLotId(parkingLotId);
            if (scopedAccountIds.isEmpty()) {
                sendNoRecipientFallback(actorAccountId, laneId, successDescription);
                return;
            }

            List<Notification> notifications = notificationSender.sendBroadcast(new BroadcastNotificationCommand(
                    false,
                    Set.of(),
                    scopedAccountIds,
                    null,
                    notificationType,
                    title,
                    message,
                    "/admin/parking-structure",
                    "parking",
                    "lanes",
                    laneId,
                    new NotificationRecipientCriteria(
                            true,
                            REQUIRED_PERMISSION_CODES,
                            Set.of(actorAccountId),
                            true
                    )
            ));
            if (notifications.isEmpty()) {
                sendNoRecipientFallback(actorAccountId, laneId, successDescription);
            }
        } catch (RuntimeException exception) {
            recordDeliveryFailure(parkingLotId, actorAccountId, laneId, exception);
            sendDeliveryFailureFallback(actorAccountId, laneId, successDescription);
        }
    }

    private void sendNoRecipientFallback(UUID actorAccountId, UUID laneId, String successDescription) {
        try {
            notificationSender.send(new SendNotificationCommand(
                    actorAccountId,
                    NotificationType.SYSTEM_NOTICE,
                    "Lane đã cập nhật",
                    successDescription + " Hiện không có nhân sự vận hành đủ điều kiện nhận thông báo.",
                    "/admin/parking-structure",
                    "parking",
                    "lanes",
                    laneId
            ));
            Metrics.counter("parking.lane.notification.fallback", "reason", "no_recipient").increment();
        } catch (RuntimeException exception) {
            recordFallbackFailure(actorAccountId, laneId, "no_recipient", exception);
        }
    }

    private void sendDeliveryFailureFallback(UUID actorAccountId, UUID laneId, String successDescription) {
        try {
            notificationSender.send(new SendNotificationCommand(
                    actorAccountId,
                    NotificationType.SYSTEM_NOTICE,
                    "Lane đã cập nhật",
                    successDescription + " Tuy nhiên, thông báo cho nhân sự vận hành chưa thể gửi.",
                    "/admin/parking-structure",
                    "parking",
                    "lanes",
                    laneId
            ));
        } catch (RuntimeException exception) {
            recordFallbackFailure(actorAccountId, laneId, "delivery_failure", exception);
        }
    }

    private void recordDeliveryFailure(
            UUID parkingLotId,
            UUID actorAccountId,
            UUID laneId,
            RuntimeException exception
    ) {
        Metrics.counter("parking.lane.notification.failed", "stage", "operations").increment();
        LOGGER.error(
                "Lane status committed but operational notification failed: parkingLotId={}, laneId={}, actorAccountId={}",
                parkingLotId,
                laneId,
                actorAccountId,
                exception
        );
    }

    private void recordFallbackFailure(
            UUID actorAccountId,
            UUID laneId,
            String reason,
            RuntimeException exception
    ) {
        Metrics.counter("parking.lane.notification.failed", "stage", "actor_fallback", "reason", reason).increment();
        LOGGER.error(
                "Lane status committed but actor fallback notification failed: laneId={}, actorAccountId={}, reason={}",
                laneId,
                actorAccountId,
                reason,
                exception
        );
    }
}
