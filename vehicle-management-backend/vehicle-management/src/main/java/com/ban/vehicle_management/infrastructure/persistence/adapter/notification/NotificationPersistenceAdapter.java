package com.ban.vehicle_management.infrastructure.persistence.adapter.notification;

import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.infrastructure.mapper.notification.NotificationPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.NotificationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.NotificationRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceAdapter implements NotificationPortOut {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final NotificationPersistenceMapper notificationPersistenceMapper;

    public NotificationPersistenceAdapter(
            NotificationRepository notificationRepository,
            AccountRepository accountRepository,
            NotificationPersistenceMapper notificationPersistenceMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.accountRepository = accountRepository;
        this.notificationPersistenceMapper = notificationPersistenceMapper;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity savedEntity = notificationRepository.saveAndFlush(
                notificationPersistenceMapper.toEntity(notification)
        );
        return notificationPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public List<Notification> saveAll(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }
        List<NotificationEntity> savedEntities = notificationRepository.saveAllAndFlush(
                notificationPersistenceMapper.toEntities(notifications)
        );
        return notificationPersistenceMapper.toDomains(savedEntities);
    }

    @Override
    public Optional<Notification> findByIdAndAccountId(UUID notificationId, UUID accountId) {
        return notificationRepository.findById(notificationId)
                .filter(notification -> accountId.equals(notification.getAccountId()))
                .map(notificationPersistenceMapper::toDomain);
    }

    @Override
    public List<Notification> findByAccountId(UUID accountId, boolean unreadOnly, Instant beforeCreatedAt, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<NotificationEntity> notifications = unreadOnly
                ? notificationRepository.findByAccountIdAndReadAtIsNullAndCreatedAtBeforeAndStatusNotOrderByCreatedAtDesc(
                accountId,
                beforeCreatedAt,
                NotificationStatus.FAILED,
                pageRequest
        )
                : notificationRepository.findByAccountIdAndCreatedAtBeforeAndStatusNotOrderByCreatedAtDesc(
                accountId,
                beforeCreatedAt,
                NotificationStatus.FAILED,
                pageRequest
        );
        return notificationPersistenceMapper.toDomains(notifications);
    }

    @Override
    public List<UUID> findExistingBroadcastIdsForAccount(UUID accountId, List<UUID> broadcastIds) {
        if (broadcastIds == null || broadcastIds.isEmpty()) {
            return List.of();
        }
        return notificationRepository.findBroadcastIdsByAccountIdAndBroadcastIdIn(accountId, broadcastIds);
    }

    @Override
    public long countUnreadByAccountId(UUID accountId) {
        return notificationRepository.countByAccountIdAndReadAtIsNullAndStatusNot(
                accountId,
                NotificationStatus.FAILED
        );
    }

    @Override
    public int markAllReadByAccountId(UUID accountId, Instant readAt) {
        return notificationRepository.markAllReadByAccountId(
                accountId,
                NotificationStatus.READ,
                NotificationStatus.FAILED,
                readAt
        );
    }

    @Override
    public List<Notification> findPendingRealtimeNotifications(UUID accountId, Instant createdAtFrom) {
        List<NotificationEntity> notifications = notificationRepository
                .findByAccountIdAndChannelAndStatusAndReadAtIsNullAndRealtimeDeliveredAtIsNullAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                        accountId,
                        NotificationChannel.WEB,
                        NotificationStatus.SENT,
                        createdAtFrom
                );
        return notificationPersistenceMapper.toDomains(notifications);
    }

    @Override
    public int markRealtimeDelivered(UUID notificationId, Instant deliveredAt) {
        return notificationRepository.markRealtimeDeliveredById(notificationId, deliveredAt);
    }

    @Override
    public int markRealtimeDelivered(List<UUID> notificationIds, Instant deliveredAt) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }
        return notificationRepository.markRealtimeDeliveredByIdIn(notificationIds, deliveredAt);
    }

    @Override
    public boolean existsAccountById(UUID accountId) {
        return accountRepository.existsById(accountId);
    }

    @Override
    public List<UUID> findActiveAccountIdsForBroadcast(
            boolean allActiveAccounts,
            List<String> roleCodes,
            List<UUID> accountIds
    ) {
        List<String> resolvedRoleCodes = roleCodes == null || roleCodes.isEmpty() ? List.of("") : roleCodes;
        List<UUID> resolvedAccountIds = accountIds == null || accountIds.isEmpty()
                ? List.of(new UUID(0L, 0L))
                : accountIds;
        return accountRepository.findActiveAccountIdsForBroadcast(
                AccountStatus.ACTIVE,
                allActiveAccounts,
                resolvedRoleCodes,
                resolvedAccountIds
        );
    }
}
