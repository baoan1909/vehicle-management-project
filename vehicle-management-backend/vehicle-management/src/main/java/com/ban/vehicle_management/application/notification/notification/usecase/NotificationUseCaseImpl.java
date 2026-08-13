package com.ban.vehicle_management.application.notification.notification.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.broadcastannouncement.port.out.BroadcastAnnouncementPortOut;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationCommandMapper;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationRealtimeMessageMapper;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.domain.notification.notification.policy.NotificationPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationUseCaseImpl implements NotificationPortIn {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final CurrentAccountPortIn currentAccountPortIn;
    private final NotificationPortOut notificationPortOut;
    private final BroadcastAnnouncementPortOut broadcastAnnouncementPortOut;
    private final NotificationRealtimeEventPublisherPortOut realtimeEventPublisher;
    private final NotificationRealtimeMessageMapper realtimeMessageMapper;
    private final NotificationCommandMapper notificationCommandMapper;
    private final NotificationPolicy notificationPolicy = new NotificationPolicy();

    public NotificationUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            NotificationPortOut notificationPortOut,
            BroadcastAnnouncementPortOut broadcastAnnouncementPortOut,
            NotificationRealtimeEventPublisherPortOut realtimeEventPublisher,
            NotificationRealtimeMessageMapper realtimeMessageMapper,
            NotificationCommandMapper notificationCommandMapper
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.notificationPortOut = notificationPortOut;
        this.broadcastAnnouncementPortOut = broadcastAnnouncementPortOut;
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
    @Transactional
    public List<Notification> getMyNotifications(boolean unreadOnly, int limit, Instant beforeCreatedAt) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        UUID accountId = currentAccount.accountId();
        materializeActiveBroadcastAnnouncementsForAccount(currentAccount);
        return notificationPortOut.findByAccountId(
                accountId,
                unreadOnly,
                beforeCreatedAt == null ? Instant.now().plusSeconds(1) : beforeCreatedAt,
                normalizeLimit(limit)
        );
    }

    @Override
    @Transactional
    public long countMyUnread() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        materializeActiveBroadcastAnnouncementsForAccount(currentAccount);
        return notificationPortOut.countUnreadByAccountId(currentAccount.accountId());
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

    private void materializeActiveBroadcastAnnouncementsForAccount(CurrentAccountAccess currentAccount) {
        if (currentAccount == null || currentAccount.accountId() == null) {
            return;
        }
        if (!AccountStatus.ACTIVE.equals(currentAccount.status())) {
            return;
        }

        Instant now = Instant.now();
        List<BroadcastAnnouncement> activeAnnouncements = broadcastAnnouncementPortOut.findActivePublishedForRole(
                currentAccount.roleCode(),
                now
        );
        if (activeAnnouncements.isEmpty()) {
            return;
        }

        List<UUID> activeBroadcastIds = activeAnnouncements.stream()
                .map(BroadcastAnnouncement::getBroadcastId)
                .filter(broadcastId -> broadcastId != null)
                .toList();
        if (activeBroadcastIds.isEmpty()) {
            return;
        }

        Set<UUID> existingBroadcastIds = notificationPortOut.findExistingBroadcastIdsForAccount(
                currentAccount.accountId(),
                activeBroadcastIds
        ).stream().collect(Collectors.toSet());

        List<Notification> missingNotifications = activeAnnouncements.stream()
                .filter(announcement -> announcement.getBroadcastId() != null)
                .filter(announcement -> !existingBroadcastIds.contains(announcement.getBroadcastId()))
                .map(announcement -> toNotificationForCurrentAccount(announcement, currentAccount.accountId(), now))
                .toList();
        if (!missingNotifications.isEmpty()) {
            notificationPortOut.saveAll(missingNotifications);
        }
    }

    private Notification toNotificationForCurrentAccount(
            BroadcastAnnouncement announcement,
            UUID accountId,
            Instant now
    ) {
        Notification notification = new Notification();
        notification.setAccountId(accountId);
        notification.setBroadcastId(announcement.getBroadcastId());
        notification.setNotificationType(announcement.getNotificationType());
        notification.setTitle(announcement.getTitle());
        notification.setMessage(announcement.getMessage());
        notification.setRedirectUrl(announcement.getRedirectUrl());
        notification.setRelatedSchema(announcement.getRelatedSchema());
        notification.setRelatedTable(announcement.getRelatedTable());
        notification.setRelatedId(announcement.getRelatedId());
        notificationPolicy.initializeWebNotification(notification, UUID.randomUUID(), now);
        return notification;
    }
}
