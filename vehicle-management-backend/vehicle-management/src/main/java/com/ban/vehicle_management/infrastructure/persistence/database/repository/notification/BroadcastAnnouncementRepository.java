package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.BroadcastAnnouncementEntity;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BroadcastAnnouncementRepository extends JpaRepository<BroadcastAnnouncementEntity, UUID> {

    List<BroadcastAnnouncementEntity> findByStatusOrderByCreatedAtDescBroadcastIdDesc(
            BroadcastAnnouncementStatus status
    );

    List<BroadcastAnnouncementEntity> findAllByOrderByCreatedAtDescBroadcastIdDesc();

    @Query("""
        select announcement
        from BroadcastAnnouncementEntity announcement
        where announcement.status = com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus.PUBLISHED
          and announcement.enabled = true
          and announcement.startAt <= :now
          and (announcement.endAt is null or announcement.endAt >= :now)
        order by announcement.displayOrder asc,
                 announcement.startAt asc,
                 announcement.publishedAt asc,
                 announcement.createdAt asc,
                 announcement.broadcastId asc
        """)
    List<BroadcastAnnouncementEntity> findActivePublishedAnnouncements(@Param("now") Instant now);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndBroadcastIdNot(String title, UUID broadcastId);
}
