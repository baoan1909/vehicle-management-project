package com.ban.vehicle_management.application.notification.userguide.model.result;

import java.util.List;
import java.util.UUID;

public record PostOnboardingReadinessResult(
        String accessRevision,
        String guideCode,
        Integer versionNumber,
        String title,
        String summary,
        UUID selectedParkingLotId,
        List<ParkingLotReadiness> parkingLots,
        List<JourneyStep> steps
) {
    public record ParkingLotReadiness(
            UUID parkingLotId,
            String name,
            String status,
            String managementState,
            boolean evaluated,
            boolean locationReady,
            boolean catalogAndPricingReady,
            boolean topologyReady,
            boolean activationReady,
            boolean operableNow,
            boolean canCheckIn,
            boolean canFindSessions,
            boolean canPrepareCheckOut,
            boolean canCollectPayment,
            boolean canCompleteCheckOut,
            boolean canHandleExceptions
    ) {
    }

    public record JourneyStep(
            int displayOrder,
            String stepKey,
            String title,
            String state,
            String description,
            String ownerHint,
            String route,
            String actionLabel,
            String linkedGuideCode
    ) {
    }
}
