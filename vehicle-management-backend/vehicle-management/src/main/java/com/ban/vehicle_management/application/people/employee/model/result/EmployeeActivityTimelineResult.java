package com.ban.vehicle_management.application.people.employee.model.result;

import java.time.Instant;
import java.util.UUID;

public record EmployeeActivityTimelineResult(
        UUID eventId,
        Instant eventTime,
        String eventType,
        String title,
        String description,
        UUID actorAccountId,
        String actorName
) {
}
