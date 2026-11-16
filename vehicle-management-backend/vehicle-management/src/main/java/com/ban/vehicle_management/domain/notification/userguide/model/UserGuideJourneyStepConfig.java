package com.ban.vehicle_management.domain.notification.userguide.model;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideJourneyStepState;
import java.util.EnumMap;
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
public class UserGuideJourneyStepConfig {
    private UUID stepId;
    private UUID guideVersionId;
    private String stepKey;
    private String readinessKey;
    private String routeTemplate;
    private String actionLabel;
    private UUID linkedGuideId;
    private Map<UserGuideJourneyStepState, UserGuideJourneyStateContent> stateContent =
            new EnumMap<>(UserGuideJourneyStepState.class);

    public Map<UserGuideJourneyStepState, UserGuideJourneyStateContent> getStateContent() {
        return stateContent == null || stateContent.isEmpty() ? Map.of() : Map.copyOf(stateContent);
    }

    public void setStateContent(Map<UserGuideJourneyStepState, UserGuideJourneyStateContent> stateContent) {
        EnumMap<UserGuideJourneyStepState, UserGuideJourneyStateContent> copy =
                new EnumMap<>(UserGuideJourneyStepState.class);
        if (stateContent != null) copy.putAll(stateContent);
        this.stateContent = copy;
    }
}
