package com.ban.vehicle_management.application.notification.notification.model;

import java.util.UUID;

public record SendNotificationCommand(
        UUID accountId,
        String title,
        String message,
        String relatedSchema,
        String relatedTable,
        UUID relatedId
) {
}
