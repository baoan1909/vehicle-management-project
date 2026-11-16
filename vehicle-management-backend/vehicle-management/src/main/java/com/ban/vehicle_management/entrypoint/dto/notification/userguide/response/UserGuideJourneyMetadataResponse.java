package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import java.util.List;
import java.util.UUID;

public record UserGuideJourneyMetadataResponse(
        List<String> readinessKeys,
        List<LinkedCoachMarkResponse> linkedCoachMarks
) {
    public record LinkedCoachMarkResponse(UUID guideId, String code, String status) {
    }
}
