package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideJourneyStepConfigEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGuideJourneyStepConfigRepository extends JpaRepository<UserGuideJourneyStepConfigEntity, UUID> {
    List<UserGuideJourneyStepConfigEntity> findAllByStepIdIn(Collection<UUID> stepIds);
}
