package com.ban.vehicle_management.application.notification.notification.model;

import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.Set;
import java.util.UUID;

public record BroadcastNotificationCommand(
        boolean allActiveAccounts,
        Set<String> roleCodes,
        Set<UUID> accountIds,
        UUID broadcastId,
        NotificationType notificationType,
        String title,
        String message,
        String redirectUrl,
        String relatedSchema,
        String relatedTable,
        UUID relatedId,
        NotificationRecipientCriteria recipientCriteria
) {
    public BroadcastNotificationCommand(
            boolean allActiveAccounts,
            Set<String> roleCodes,
            Set<UUID> accountIds,
            UUID broadcastId,
            NotificationType notificationType,
            String title,
            String message,
            String redirectUrl,
            String relatedSchema,
            String relatedTable,
            UUID relatedId
    ) {
        this(
                allActiveAccounts,
                roleCodes,
                accountIds,
                broadcastId,
                notificationType,
                title,
                message,
                redirectUrl,
                relatedSchema,
                relatedTable,
                relatedId,
                null
        );
    }
}
