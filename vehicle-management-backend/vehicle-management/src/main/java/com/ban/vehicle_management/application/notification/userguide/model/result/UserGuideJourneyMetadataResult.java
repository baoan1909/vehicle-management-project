package com.ban.vehicle_management.application.notification.userguide.model.result;

import java.util.List;
import java.util.UUID;

public record UserGuideJourneyMetadataResult(
        List<String> readinessKeys,
        List<LinkedCoachMark> linkedCoachMarks
) {
    public record LinkedCoachMark(UUID guideId, String code, String status) {
    }
}
