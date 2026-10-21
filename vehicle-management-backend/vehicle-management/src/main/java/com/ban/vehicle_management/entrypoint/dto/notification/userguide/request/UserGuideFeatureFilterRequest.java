package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

public record UserGuideFeatureFilterRequest(
        Boolean enabled,
        String keyword
) {
}
