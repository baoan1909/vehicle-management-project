package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import jakarta.validation.constraints.Size;

public record UpdateUserGuideFeatureRequest(
        @Size(max = 150) String name,
        @Size(max = 1000) String description,
        Boolean enabled
) {
}
