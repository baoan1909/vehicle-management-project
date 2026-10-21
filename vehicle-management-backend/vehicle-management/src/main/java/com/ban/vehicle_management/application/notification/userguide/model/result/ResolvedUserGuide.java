package com.ban.vehicle_management.application.notification.userguide.model.result;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import java.util.Map;

public record ResolvedUserGuide(
        UserGuide guide,
        UserGuideVersion version,
        UserGuideUserState userState,
        Map<String, String> mediaUrls
) {
}
