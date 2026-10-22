package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideFeatureEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserGuideFeatureRepository extends JpaRepository<UserGuideFeatureEntity, UUID>,
        JpaSpecificationExecutor<UserGuideFeatureEntity> {

    Optional<UserGuideFeatureEntity> findByCode(String code);

    boolean existsByCode(String code);
}
