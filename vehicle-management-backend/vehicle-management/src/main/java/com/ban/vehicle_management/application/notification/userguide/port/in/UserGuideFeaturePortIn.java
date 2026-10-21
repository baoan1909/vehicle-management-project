package com.ban.vehicle_management.application.notification.userguide.port.in;

import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserGuideFeaturePortIn {

    UserGuideFeature createFeature(UserGuideFeature feature);

    UserGuideFeature updateFeature(java.util.UUID featureId, UserGuideFeature feature);

    UserGuideFeature getFeature(java.util.UUID featureId);

    Page<UserGuideFeature> listFeatures(Boolean enabled, String keyword, Pageable pageable);
}
