package com.ban.vehicle_management.entrypoint.dto.notification.notification.request;

import java.time.Instant;

public record NotificationFilterRequest(
        Boolean unreadOnly,
        Integer limit,
        Instant beforeCreatedAt
) {
}
