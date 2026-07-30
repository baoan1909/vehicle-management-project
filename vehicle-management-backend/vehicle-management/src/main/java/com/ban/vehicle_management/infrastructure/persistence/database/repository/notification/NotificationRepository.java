package com.ban.vehicle_management.infrastructure.persistence.database.repository.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.NotificationEntity;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByAccountIdAndCreatedAtBeforeAndStatusNotOrderByCreatedAtDesc(
            UUID accountId,
            Instant createdAt,
            NotificationStatus status,
            Pageable pageable
    );

    List<NotificationEntity> findByAccountIdAndReadAtIsNullAndCreatedAtBeforeAndStatusNotOrderByCreatedAtDesc(
            UUID accountId,
            Instant createdAt,
            NotificationStatus status,
            Pageable pageable
    );

    long countByAccountIdAndReadAtIsNullAndStatusNot(UUID accountId, NotificationStatus status);

    List<NotificationEntity>
    findByAccountIdAndChannelAndStatusAndReadAtIsNullAndRealtimeDeliveredAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            UUID accountId,
            NotificationChannel channel,
            NotificationStatus status,
            Instant createdAt
    );

    @Modifying
    @Query("""
            update NotificationEntity notification
            set notification.status = :readStatus,
                notification.readAt = :readAt
            where notification.accountId = :accountId
              and notification.readAt is null
              and notification.status <> :failedStatus
            """)
    int markAllReadByAccountId(
            @Param("accountId") UUID accountId,
            @Param("readStatus") NotificationStatus readStatus,
            @Param("failedStatus") NotificationStatus failedStatus,
            @Param("readAt") Instant readAt
    );

    @Modifying
    @Query("""
            update NotificationEntity notification
            set notification.realtimeDeliveredAt = :deliveredAt
            where notification.notificationId = :notificationId
              and notification.realtimeDeliveredAt is null
            """)
    int markRealtimeDeliveredById(
            @Param("notificationId") UUID notificationId,
            @Param("deliveredAt") Instant deliveredAt
    );

    @Modifying
    @Query("""
            update NotificationEntity notification
            set notification.realtimeDeliveredAt = :deliveredAt
            where notification.notificationId in :notificationIds
              and notification.realtimeDeliveredAt is null
            """)
    int markRealtimeDeliveredByIdIn(
            @Param("notificationIds") List<UUID> notificationIds,
            @Param("deliveredAt") Instant deliveredAt
    );
}


