package com.ban.vehicle_management.application.notification.notification.port.out;

import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPortOut {

    Notification save(Notification notification);

    List<Notification> saveAll(List<Notification> notifications);

    Optional<Notification> findByIdAndAccountId(UUID notificationId, UUID accountId);

    List<Notification> findByAccountId(UUID accountId, boolean unreadOnly, Instant beforeCreatedAt, int limit);

    long countUnreadByAccountId(UUID accountId);

    int markAllReadByAccountId(UUID accountId, Instant readAt);

    List<Notification> findPendingRealtimeNotifications(UUID accountId, Instant createdAtFrom);

    int markRealtimeDelivered(UUID notificationId, Instant deliveredAt);

    int markRealtimeDelivered(List<UUID> notificationIds, Instant deliveredAt);

    boolean existsAccountById(UUID accountId);

    List<UUID> findActiveAccountIdsForBroadcast(
            boolean allActiveAccounts,
            List<String> roleCodes,
            List<UUID> accountIds
    );
}
