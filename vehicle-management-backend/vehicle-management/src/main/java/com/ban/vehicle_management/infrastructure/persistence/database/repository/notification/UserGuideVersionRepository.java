package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideVersionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGuideVersionRepository extends JpaRepository<UserGuideVersionEntity, UUID> {

    Optional<UserGuideVersionEntity> findByGuideIdAndVersionNumber(UUID guideId, Integer versionNumber);

    List<UserGuideVersionEntity> findByGuideIdOrderByVersionNumberDesc(UUID guideId);
}
