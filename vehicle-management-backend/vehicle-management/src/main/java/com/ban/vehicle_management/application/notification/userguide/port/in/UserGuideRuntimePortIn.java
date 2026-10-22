package com.ban.vehicle_management.application.notification.userguide.port.in;

import com.ban.vehicle_management.application.notification.userguide.model.result.ResolvedUserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideEventType;
import java.util.List;
import java.util.UUID;

public interface UserGuideRuntimePortIn {

    List<ResolvedUserGuide> resolveGuides(String featureCode, String contextKey);

    ResolvedUserGuide getPublishedGuide(String guideCode);

    UserGuideUserState recordEvent(UUID guideId, UserGuideEventType event, Integer versionNumber, UUID stepId);
}
