package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateUserGuideVersionRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String summary,
        @Size(max = 1000) String deepLink,
        UserGuideAudienceMode audienceMode,
        @Min(1) Integer cloneFromVersionNumber,
        @Size(max = 50) List<@Valid UserGuideStepPayload> steps,
        @Size(max = 100) List<@Valid UserGuideAudiencePayload> audiences
) {
}
