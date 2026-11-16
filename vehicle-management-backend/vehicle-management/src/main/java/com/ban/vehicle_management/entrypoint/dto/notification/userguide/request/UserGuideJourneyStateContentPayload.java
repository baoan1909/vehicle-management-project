package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import jakarta.validation.constraints.Size;

public record UserGuideJourneyStateContentPayload(
        @Size(max = 10000) String description,
        @Size(max = 500) String ownerHint,
        @Size(max = 100) String actionLabel
) {
}
