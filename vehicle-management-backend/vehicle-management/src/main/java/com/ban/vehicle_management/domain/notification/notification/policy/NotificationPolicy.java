package com.ban.vehicle_management.domain.notification.notification.policy;

import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class NotificationPolicy {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int REDIRECT_URL_MAX_LENGTH = 1000;
    private static final int RELATED_SCHEMA_MAX_LENGTH = 50;
    private static final int RELATED_TABLE_MAX_LENGTH = 80;

    public SendNotificationCommand normalizeCommand(SendNotificationCommand command) {
        if (command == null) {
            throw new BadRequestException("command must not be null");
        }
        if (command.accountId() == null) {
            throw new BadRequestException("accountId must not be null");
        }
        if (command.notificationType() == null) {
            throw new BadRequestException("notificationType must not be null");
        }
        return new SendNotificationCommand(
                command.accountId(),
                command.notificationType(),
                TextValidationUtils.normalizeRequiredText(command.title(), "title", TITLE_MAX_LENGTH),
                TextValidationUtils.normalizeRequiredText(command.message(), "message", 0),
                TextValidationUtils.normalizeNullableText(
                        command.relatedSchema(),
                        "relatedSchema",
                        RELATED_SCHEMA_MAX_LENGTH
                ),
                TextValidationUtils.normalizeNullableText(
                        command.relatedTable(),
                        "relatedTable",
                        RELATED_TABLE_MAX_LENGTH
                ),
                command.relatedId()
        );
    }

    public BroadcastNotificationCommand normalizeBroadcastCommand(BroadcastNotificationCommand command) {
        if (command == null) {
            throw new BadRequestException("command must not be null");
        }

        Set<UUID> accountIds = normalizeAccountIds(command.accountIds());
        Set<String> roleCodes = normalizeRoleCodes(command.roleCodes());
        if (!command.allActiveAccounts() && accountIds.isEmpty() && roleCodes.isEmpty()) {
            throw new BadRequestException("Broadcast notification requires at least one recipient target");
        }

        return new BroadcastNotificationCommand(
                command.allActiveAccounts(),
                roleCodes,
                accountIds,
                command.broadcastId(),
                requireNotificationType(command.notificationType()),
                TextValidationUtils.normalizeRequiredText(command.title(), "title", TITLE_MAX_LENGTH),
                TextValidationUtils.normalizeRequiredText(command.message(), "message", 0),
                TextValidationUtils.normalizeNullableText(
                        command.redirectUrl(),
                        "redirectUrl",
                        REDIRECT_URL_MAX_LENGTH
                ),
                TextValidationUtils.normalizeNullableText(
                        command.relatedSchema(),
                        "relatedSchema",
                        RELATED_SCHEMA_MAX_LENGTH
                ),
                TextValidationUtils.normalizeNullableText(
                        command.relatedTable(),
                        "relatedTable",
                        RELATED_TABLE_MAX_LENGTH
                ),
                command.relatedId()
        );
    }

    public void initializeWebNotification(Notification notification, UUID notificationId, Instant now) {
        if (notification == null) {
            throw new BadRequestException("notification must not be null");
        }
        if (notification.getAccountId() == null) {
            throw new BadRequestException("accountId must not be null");
        }
        notification.setNotificationId(notificationId);
        notification.setChannel(NotificationChannel.WEB);
        if (notification.getNotificationType() == null) {
            notification.setNotificationType(NotificationType.SYSTEM_NOTICE);
        }
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(now);
        notification.setReadAt(null);
        notification.setRealtimeDeliveredAt(null);
    }

    public void markRead(Notification notification, Instant readAt) {
        if (notification == null) {
            throw new BadRequestException("notification must not be null");
        }
        if (notification.getReadAt() != null || notification.getStatus() == NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            return;
        }
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(readAt);
    }

    private Set<UUID> normalizeAccountIds(Set<UUID> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(accountIds);
    }

    private Set<String> normalizeRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedRoleCodes = new LinkedHashSet<>();
        roleCodes.forEach(roleCode -> normalizedRoleCodes.add(
                TextValidationUtils.normalizeCode(roleCode, "roleCode", 50)
        ));
        return normalizedRoleCodes;
    }

    private NotificationType requireNotificationType(NotificationType notificationType) {
        if (notificationType == null) {
            throw new BadRequestException("notificationType must not be null");
        }
        return notificationType;
    }
}
