package com.ban.vehicle_management.application.notification.notification.model;

import java.util.Set;
import java.util.UUID;

public record NotificationRecipientCriteria(
        boolean requireBusinessAccess,
        Set<String> requiredAnyPermissionCodes,
        Set<UUID> excludedAccountIds,
        boolean allowNoRecipients
) {
}
