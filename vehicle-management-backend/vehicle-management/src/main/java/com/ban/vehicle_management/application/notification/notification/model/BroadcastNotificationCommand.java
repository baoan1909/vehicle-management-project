package com.ban.vehicle_management.application.notification.notification.model;

import java.util.Set;
import java.util.UUID;

public record BroadcastNotificationCommand(
        boolean allActiveAccounts,
        Set<String> roleCodes,
        Set<UUID> accountIds,
        String title,
        String message,
        String relatedSchema,
        String relatedTable,
        UUID relatedId
) {
}
