package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.BroadcastAnnouncementEntity;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastAnnouncementRepository extends JpaRepository<BroadcastAnnouncementEntity, UUID> {

    List<BroadcastAnnouncementEntity> findByStatusOrderByCreatedAtDescBroadcastIdDesc(
            BroadcastAnnouncementStatus status
    );

    List<BroadcastAnnouncementEntity> findAllByOrderByCreatedAtDescBroadcastIdDesc();
}
