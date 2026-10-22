package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceTargetType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UserGuideAudiencePayload(
        @NotNull UserGuideAudienceTargetType targetType,
        UUID roleId,
        UUID permissionId
) {
}
