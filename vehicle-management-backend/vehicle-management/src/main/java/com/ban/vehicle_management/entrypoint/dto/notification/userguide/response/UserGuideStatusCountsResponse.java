package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

public record UserGuideStatusCountsResponse(
        long all,
        long draft,
        long published,
        long inactive,
        long archived
) {
}
