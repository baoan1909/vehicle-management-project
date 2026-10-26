package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AccountAccessInvalidationUseCaseImplTest {

    @Mock
    private RequiresNewNotificationSender notificationSender;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void invalidatingAccountPersistsReplayableSignalWithoutPermissionPayload() {
        UUID accountId = UUID.randomUUID();
        UUID relatedId = UUID.randomUUID();
        AccountAccessInvalidationUseCaseImpl useCase = new AccountAccessInvalidationUseCaseImpl(notificationSender);

        useCase.invalidateAccount(accountId, "Role changed", "iam", "accounts", relatedId);

        ArgumentCaptor<SendNotificationCommand> captor = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(notificationSender).send(captor.capture());
        SendNotificationCommand command = captor.getValue();
        assertEquals(accountId, command.accountId());
        assertEquals(NotificationType.ACCOUNT_ACCESS_CHANGED, command.notificationType());
        assertEquals("iam", command.relatedSchema());
        assertEquals("accounts", command.relatedTable());
        assertEquals(relatedId, command.relatedId());
    }

    @Test
    void invalidatingNullAccountDoesNothing() {
        AccountAccessInvalidationUseCaseImpl useCase = new AccountAccessInvalidationUseCaseImpl(notificationSender);

        useCase.invalidateAccount(null, "Role changed", "iam", "accounts", UUID.randomUUID());

        verify(notificationSender, never()).send(any());
    }

    @Test
    void invalidatingRoleUsesOneBatchBroadcastAndAllowsEmptyAudience() {
        UUID roleId = UUID.randomUUID();
        AccountAccessInvalidationUseCaseImpl useCase = new AccountAccessInvalidationUseCaseImpl(notificationSender);

        useCase.invalidateActiveAccountsForRole(" custom_ops ", roleId, "Permissions changed");

        ArgumentCaptor<BroadcastNotificationCommand> captor =
                ArgumentCaptor.forClass(BroadcastNotificationCommand.class);
        verify(notificationSender).sendBroadcast(captor.capture());
        BroadcastNotificationCommand command = captor.getValue();
        assertEquals(NotificationType.ACCOUNT_ACCESS_CHANGED, command.notificationType());
        assertEquals(java.util.Set.of("CUSTOM_OPS"), command.roleCodes());
        assertEquals(roleId, command.relatedId());
        assertEquals(true, command.recipientCriteria().allowNoRecipients());
    }

    @Test
    void invalidationIsSentOnlyAfterBusinessTransactionCommits() {
        UUID accountId = UUID.randomUUID();
        AccountAccessInvalidationUseCaseImpl useCase = new AccountAccessInvalidationUseCaseImpl(notificationSender);
        TransactionSynchronizationManager.initSynchronization();

        useCase.invalidateAccount(accountId, "Role changed", "iam", "accounts", accountId);

        verify(notificationSender, never()).send(any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(notificationSender).send(any(SendNotificationCommand.class));
    }

    @Test
    void invalidationIsNotSentWhenBusinessTransactionRollsBack() {
        UUID accountId = UUID.randomUUID();
        AccountAccessInvalidationUseCaseImpl useCase = new AccountAccessInvalidationUseCaseImpl(notificationSender);
        TransactionSynchronizationManager.initSynchronization();

        useCase.invalidateAccount(accountId, "Role changed", "iam", "accounts", accountId);
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(notificationSender, never()).send(any());
    }
}
