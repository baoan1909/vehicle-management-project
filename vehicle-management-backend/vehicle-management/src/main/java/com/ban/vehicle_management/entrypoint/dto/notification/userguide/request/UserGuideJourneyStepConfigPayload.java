package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideJourneyStepState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;

public record UserGuideJourneyStepConfigPayload(
        @NotBlank @Size(max = 120) String stepKey,
        @NotBlank @Size(max = 120) String readinessKey,
        @Size(max = 1000) String routeTemplate,
        @Size(max = 100) String actionLabel,
        UUID linkedGuideId,
        Map<UserGuideJourneyStepState, @Valid UserGuideJourneyStateContentPayload> stateContent
) {
}
