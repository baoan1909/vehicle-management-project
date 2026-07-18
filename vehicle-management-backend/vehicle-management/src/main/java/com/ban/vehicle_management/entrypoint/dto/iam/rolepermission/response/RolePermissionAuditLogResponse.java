package com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.response;

import java.util.Map;
import java.util.UUID;

public record RolePermissionAuditLogResponse(
        UUID eventId,
        String eventTime,
        String action,
        UUID actorAccountId,
        String actorUsername,
        String actorFullName,
        Map<String, Object> oldData,
        Map<String, Object> newData
) {
}
