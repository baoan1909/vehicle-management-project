package com.ban.vehicle_management.application.notification.notification.model;

import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import java.util.UUID;

public record NotificationRealtimeMessage(
        UUID notificationId,
        UUID accountId,
        NotificationChannel channel,
        String title,
        String message,
        NotificationStatus status,
        String sentAt,
        String readAt,
        String relatedSchema,
        String relatedTable,
        UUID relatedId,
        String createdAt
) {
}
