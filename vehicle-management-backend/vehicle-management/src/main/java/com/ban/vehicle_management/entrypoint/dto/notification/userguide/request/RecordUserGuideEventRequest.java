package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideEventType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordUserGuideEventRequest(
        @NotNull UserGuideEventType event,
        @NotNull @Min(1) Integer versionNumber,
        UUID stepId
) {
}


