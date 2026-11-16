package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostOnboardingReadinessResponse {
    private String accessRevision;
    private String guideCode;
    private Integer versionNumber;
    private String title;
    private String summary;
    private UUID selectedParkingLotId;
    private List<ParkingLotReadinessResponse> parkingLots;
    private List<JourneyStepResponse> steps;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParkingLotReadinessResponse {
        private UUID parkingLotId;
        private String name;
        private String status;
        private String managementState;
        private boolean evaluated;
        private boolean locationReady;
        private boolean catalogAndPricingReady;
        private boolean topologyReady;
        private boolean activationReady;
        private boolean operableNow;
        private boolean canCheckIn;
        private boolean canFindSessions;
        private boolean canPrepareCheckOut;
        private boolean canCollectPayment;
        private boolean canCompleteCheckOut;
        private boolean canHandleExceptions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JourneyStepResponse {
        private int displayOrder;
        private String stepKey;
        private String title;
        private String state;
        private String description;
        private String ownerHint;
        private String route;
        private String actionLabel;
        private String linkedGuideCode;
    }
}
