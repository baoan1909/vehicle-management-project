package com.ban.vehicle_management.entrypoint.dto.iam.rolepermission.response;

import java.time.Instant;

import java.util.Map;
import java.util.UUID;

public record RolePermissionAuditLogResponse(
        UUID eventId,
        Instant eventTime,
        String action,
        UUID actorAccountId,
        String actorUsername,
        String actorFullName,
        Map<String, Object> oldData,
        Map<String, Object> newData
) {
}
