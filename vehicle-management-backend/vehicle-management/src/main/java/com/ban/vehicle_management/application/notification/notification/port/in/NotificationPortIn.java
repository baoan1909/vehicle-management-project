package com.ban.vehicle_management.application.notification.notification.port.in;

import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationPortIn {

    Notification sendWebNotification(SendNotificationCommand command);

    List<Notification> getMyNotifications(boolean unreadOnly, int limit, Instant beforeCreatedAt);

    long countMyUnread();

    Notification markMyNotificationAsRead(UUID notificationId);

    void markAllMyNotificationsAsRead();
}
