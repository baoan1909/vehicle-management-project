package com.ban.vehicle_management.application.iam.rolepermission.model.result;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RolePermissionAuditLogResult(
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
