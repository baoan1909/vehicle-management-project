package com.ban.vehicle_management.application.notification.userguide.port.out;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGuideVersionPortOut {

    UserGuideVersion save(UserGuideVersion version);

    Optional<UserGuideVersion> findById(UUID guideVersionId);

    Optional<UserGuideVersion> findByGuideIdAndVersionNumber(UUID guideId, Integer versionNumber);

    List<UserGuideVersion> findByGuideIdOrderByVersionNumberDesc(UUID guideId);
}
