package com.ban.vehicle_management.entrypoint.dto.notification.userguide.request;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import java.util.UUID;

public record UserGuideFilterRequest(
        UUID featureId,
        UserGuideStatus status,
        UserGuideDisplayType displayType,
        String keyword
) {
}
