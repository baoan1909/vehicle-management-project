package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideAudienceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGuideAudienceRepository extends JpaRepository<UserGuideAudienceEntity, UUID> {

    List<UserGuideAudienceEntity> findByGuideVersionIdOrderByCreatedAtAscAudienceIdAsc(UUID guideVersionId);

    void deleteAllByGuideVersionId(UUID guideVersionId);
}
