package com.ban.vehicle_management.application.notification.userguide.port.in;

import com.ban.vehicle_management.application.notification.userguide.model.result.PostOnboardingReadinessResult;
import java.util.UUID;

public interface PostOnboardingReadinessPortIn {
    PostOnboardingReadinessResult resolveJourney(String featureCode, String contextKey, UUID parkingLotId);

    default PostOnboardingReadinessResult getReadiness(UUID parkingLotId) {
        return resolveJourney("OPERATIONS_ONBOARDING", "/ADMIN/HEADER", parkingLotId);
    }
}
