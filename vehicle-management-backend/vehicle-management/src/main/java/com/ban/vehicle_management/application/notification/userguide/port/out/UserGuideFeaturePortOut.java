package com.ban.vehicle_management.application.notification.userguide.port.out;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserGuideFeaturePortOut {

    UserGuideFeature save(UserGuideFeature feature);

    Optional<UserGuideFeature> findById(UUID featureId);

    Optional<UserGuideFeature> findByCode(String code);

    boolean existsByCode(String code);

    Page<UserGuideFeature> findPage(Boolean enabled, String keyword, Pageable pageable);
}
