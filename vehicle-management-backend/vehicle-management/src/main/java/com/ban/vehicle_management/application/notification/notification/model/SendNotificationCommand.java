package com.ban.vehicle_management.application.notification.notification.model;

import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.UUID;

public record SendNotificationCommand(
        UUID accountId,
        NotificationType notificationType,
        String title,
        String message,
        String relatedSchema,
        String relatedTable,
        UUID relatedId
) {
}
