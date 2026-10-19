package com.ban.vehicle_management.application.notification.userguide.port.out;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserGuidePortOut {

    UserGuide save(UserGuide guide);

    Optional<UserGuide> findById(UUID guideId);

    Optional<UserGuide> findByIdForUpdate(UUID guideId);

    Optional<UserGuide> findByCode(String code);

    boolean existsByCode(String code);

    List<UserGuide> findByFeatureIdAndStatus(UUID featureId, UserGuideStatus status);

    Page<UserGuide> findPage(
            UUID featureId,
            UserGuideStatus status,
            UserGuideDisplayType displayType,
            String keyword,
            Pageable pageable
    );
}
