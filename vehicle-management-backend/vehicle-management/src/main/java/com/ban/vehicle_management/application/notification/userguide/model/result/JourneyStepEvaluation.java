package com.ban.vehicle_management.application.notification.userguide.model.result;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideJourneyStepState;

public record JourneyStepEvaluation(UserGuideJourneyStepState state, boolean actionAllowed) {
}
