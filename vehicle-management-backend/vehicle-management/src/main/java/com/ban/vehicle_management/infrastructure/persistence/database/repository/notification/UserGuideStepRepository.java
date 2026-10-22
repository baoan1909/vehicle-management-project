package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideStepEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGuideStepRepository extends JpaRepository<UserGuideStepEntity, UUID> {

    List<UserGuideStepEntity> findByGuideVersionIdOrderByDisplayOrderAsc(UUID guideVersionId);

    void deleteAllByGuideVersionId(UUID guideVersionId);
}
