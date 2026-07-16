package com.ban.vehicle_management.entrypoint.dto.people.employee.response;

import java.time.Instant;
import java.util.UUID;

public record EmployeeActivityTimelineResponse(
        UUID eventId,
        Instant eventTime,
        String eventType,
        String title,
        String description,
        UUID actorAccountId,
        String actorName
) {
}
