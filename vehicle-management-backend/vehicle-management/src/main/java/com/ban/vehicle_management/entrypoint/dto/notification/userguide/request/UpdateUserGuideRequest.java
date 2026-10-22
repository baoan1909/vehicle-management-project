package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideReplayPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateUserGuideRequest(
        @Size(max = 120) String contextKey,
        UserGuideDisplayType displayType,
        UserGuideReplayPolicy replayPolicy,
        @Min(0) Integer priority,
        @Min(0) Integer displayOrder,
        Instant startAt,
        Instant endAt,
        @NotNull Long lockVersion
) {
}
