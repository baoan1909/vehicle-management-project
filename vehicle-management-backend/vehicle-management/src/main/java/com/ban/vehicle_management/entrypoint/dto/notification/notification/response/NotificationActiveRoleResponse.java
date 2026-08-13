package com.ban.vehicle_management.entrypoint.dto.notification.notification.response;

import java.util.UUID;

public record NotificationActiveRoleResponse(
        UUID roleId,
        String code,
        String name
) {
}
