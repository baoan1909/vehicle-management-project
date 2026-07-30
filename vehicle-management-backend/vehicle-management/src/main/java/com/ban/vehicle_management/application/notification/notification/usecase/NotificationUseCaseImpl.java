package com.ban.vehicle_management.application.notification.notification.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationCommandMapper;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationRealtimeMessageMapper;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.domain.notification.notification.policy.NotificationPolicy;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationUseCaseImpl implements NotificationPortIn {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final CurrentAccountPortIn currentAccountPortIn;
    private final NotificationPortOut notificationPortOut;
    private final NotificationRealtimeEventPublisherPortOut realtimeEventPublisher;
    private final NotificationRealtimeMessageMapper realtimeMessageMapper;
    private final NotificationCommandMapper notificationCommandMapper;
    private final NotificationPolicy notificationPolicy = new NotificationPolicy();

    public NotificationUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            NotificationPortOut notificationPortOut,
            NotificationRealtimeEventPublisherPortOut realtimeEventPublisher,
            NotificationRealtimeMessageMapper realtimeMessageMapper,
            NotificationCommandMapper notificationCommandMapper
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.notificationPortOut = notificationPortOut;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.realtimeMessageMapper = realtimeMessageMapper;
        this.notificationCommandMapper = notificationCommandMapper;
    }

    @Override
    @Transactional
    public Notification sendWebNotification(SendNotificationCommand command) {
        SendNotificationCommand normalizedCommand = notificationPolicy.normalizeCommand(command);
        if (!notificationPortOut.existsAccountById(normalizedCommand.accountId())) {
            throw new NotFoundException("Account not found");
        }

        Instant now = Instant.now();
        Notification notification = notificationCommandMapper.toDomain(normalizedCommand);
        notificationPolicy.initializeWebNotification(notification, UUID.randomUUID(), now);

        Notification savedNotification = notificationPortOut.save(notification);
        TransactionalEvents.runAfterCommit(() ->
                realtimeEventPublisher.publish(realtimeMessageMapper.toRealtimeMessage(savedNotification))
        );
        return savedNotification;
    }

    @Override
    @Transactional
    public List<Notification> sendBroadcastWebNotification(BroadcastNotificationCommand command) {
        BroadcastNotificationCommand normalizedCommand = notificationPolicy.normalizeBroadcastCommand(command);
        List<UUID> recipientAccountIds = notificationPortOut.findActiveAccountIdsForBroadcast(
                normalizedCommand.allActiveAccounts(),
                normalizedCommand.roleCodes().stream().toList(),
                normalizedCommand.accountIds().stream().toList()
        );
        if (recipientAccountIds.isEmpty()) {
            throw new BadRequestException("Broadcast notification has no eligible active recipients");
        }

        Instant now = Instant.now();
        List<Notification> notifications = recipientAccountIds.stream()
                .map(accountId -> {
                    Notification notification = notificationCommandMapper.toDomain(normalizedCommand, accountId);
                    notificationPolicy.initializeWebNotification(notification, UUID.randomUUID(), now);
                    return notification;
                })
                .toList();
        List<Notification> savedNotifications = notificationPortOut.saveAll(notifications);
        TransactionalEvents.runAfterCommit(() -> savedNotifications.stream()
                .map(realtimeMessageMapper::toRealtimeMessage)
                .forEach(realtimeEventPublisher::publish)
        );
        return savedNotifications;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications(boolean unreadOnly, int limit, Instant beforeCreatedAt) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        return notificationPortOut.findByAccountId(
                accountId,
                unreadOnly,
                beforeCreatedAt == null ? Instant.now().plusSeconds(1) : beforeCreatedAt,
                normalizeLimit(limit)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyUnread() {
        return notificationPortOut.countUnreadByAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
    }

    @Override
    @Transactional
    public Notification markMyNotificationAsRead(UUID notificationId) {
        if (notificationId == null) {
            throw new BadRequestException("notificationId must not be null");
        }
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Notification notification = notificationPortOut.findByIdAndAccountId(notificationId, accountId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notificationPolicy.markRead(notification, Instant.now());
        return notificationPortOut.save(notification);
    }

    @Override
    @Transactional
    public void markAllMyNotificationsAsRead() {
        notificationPortOut.markAllReadByAccountId(
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now()
        );
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
