package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.AccountAccessInvalidationPortIn;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRecipientCriteria;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountAccessInvalidationUseCaseImpl implements AccountAccessInvalidationPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountAccessInvalidationUseCaseImpl.class);
    private final RequiresNewNotificationSender notificationSender;

    public AccountAccessInvalidationUseCaseImpl(RequiresNewNotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    @Override
    public void invalidateAccount(
            UUID accountId,
            String reason,
            String relatedSchema,
            String relatedTable,
            UUID relatedId
    ) {
        if (accountId == null) {
            return;
        }
        SendNotificationCommand command = new SendNotificationCommand(
                accountId,
                NotificationType.ACCOUNT_ACCESS_CHANGED,
                "Quyền truy cập đã thay đổi",
                normalizeReason(reason),
                null,
                relatedSchema,
                relatedTable,
                relatedId
        );
        TransactionalEvents.runAfterCommit(() -> sendSafely(command));
    }

    @Override
    public void invalidateActiveAccountsForRole(String roleCode, UUID roleId, String reason) {
        String normalizedRoleCode = TextValidationUtils.normalizeCode(roleCode, "roleCode", 50);
        BroadcastNotificationCommand command = new BroadcastNotificationCommand(
                false,
                Set.of(normalizedRoleCode),
                Set.of(),
                null,
                NotificationType.ACCOUNT_ACCESS_CHANGED,
                "Quyền truy cập đã thay đổi",
                normalizeReason(reason),
                null,
                "iam",
                "roles",
                roleId,
                new NotificationRecipientCriteria(false, Set.of(), Set.of(), true)
        );
        TransactionalEvents.runAfterCommit(() -> sendBroadcastSafely(command));
    }

    private String normalizeReason(String reason) {
        return TextValidationUtils.normalizeRequiredText(reason, "reason", 500);
    }

    private void sendSafely(SendNotificationCommand command) {
        try {
            notificationSender.send(command);
        } catch (RuntimeException exception) {
            LOGGER.warn("Access invalidation notification failed for account {}", command.accountId(), exception);
        }
    }

    private void sendBroadcastSafely(BroadcastNotificationCommand command) {
        try {
            notificationSender.sendBroadcast(command);
        } catch (RuntimeException exception) {
            LOGGER.warn("Access invalidation broadcast failed for roles {}", command.roleCodes(), exception);
        }
    }
}
