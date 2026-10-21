package com.ban.vehicle_management.application.notification.userguide.port.in;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserGuideAdminPortIn {

    UserGuide createGuide(UserGuide guide, UserGuideVersion initialVersion);

    UserGuide createGuide(UserGuide guide, UserGuideVersion initialVersion, String featureCode);

    UserGuide updateGuide(UUID guideId, UserGuide guide, Long lockVersion);

    UserGuide getGuide(UUID guideId);

    Page<UserGuide> listGuides(
            UUID featureId,
            UserGuideStatus status,
            UserGuideDisplayType displayType,
            String keyword,
            Pageable pageable
    );

    UserGuideVersion createDraftVersion(UUID guideId, UserGuideVersion version, Integer cloneFromVersionNumber);

    UserGuideVersion updateDraftVersion(UUID guideId, Integer versionNumber, UserGuideVersion version, Long lockVersion);

    UserGuideVersion getVersion(UUID guideId, Integer versionNumber);

    List<UserGuideVersion> listVersions(UUID guideId);

    UserGuideVersion publishVersion(UUID guideId, Integer versionNumber);

    UserGuide archiveGuide(UUID guideId);
}
