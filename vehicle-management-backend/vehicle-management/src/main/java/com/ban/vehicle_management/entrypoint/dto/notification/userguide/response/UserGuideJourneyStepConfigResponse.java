package com.ban.vehicle_management.entrypoint.dto.notification.userguide.response;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideJourneyStepState;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideJourneyStepConfigResponse {
    private String stepKey;
    private String readinessKey;
    private String routeTemplate;
    private String actionLabel;
    private UUID linkedGuideId;
    private Map<UserGuideJourneyStepState, UserGuideJourneyStateContentResponse> stateContent;
}
