package com.ban.vehicle_management.application.notification.notification.model;

import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.UUID;

public record NotificationRealtimeMessage(
        UUID notificationId,
        UUID accountId,
        UUID broadcastId,
        NotificationChannel channel,
        NotificationType notificationType,
        String title,
        String message,
        NotificationStatus status,
        java.time.Instant sentAt,
        java.time.Instant readAt,
        String redirectUrl,
        String relatedSchema,
        String relatedTable,
        UUID relatedId,
        java.time.Instant createdAt
) {
}
