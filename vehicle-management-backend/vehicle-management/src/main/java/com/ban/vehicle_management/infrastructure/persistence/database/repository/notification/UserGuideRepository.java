package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideEntity;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGuideRepository extends JpaRepository<UserGuideEntity, UUID>,
        JpaSpecificationExecutor<UserGuideEntity> {

    Optional<UserGuideEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<UserGuideEntity> findByFeatureIdAndStatus(UUID featureId, UserGuideStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select guide from UserGuideEntity guide where guide.guideId = :guideId")
    Optional<UserGuideEntity> findByIdForUpdate(@Param("guideId") UUID guideId);
}
