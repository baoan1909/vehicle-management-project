package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedAccountArchiveResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionArchivePortOut;
import com.ban.vehicle_management.application.iam.account.port.out.DeletedIdentityFingerprintPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AccountDeletionArchiveUseCaseImplTest {

    @Test
    void requiresPermissionAndAuditsEveryDeletedAccountRead() {
        AccountDeletionArchivePortOut archivePort = mock(AccountDeletionArchivePortOut.class);
        DeletedIdentityFingerprintPortOut fingerprintPort = mock(DeletedIdentityFingerprintPortOut.class);
        CurrentAccountPortIn currentAccount = mock(CurrentAccountPortIn.class);
        AuditLogPortOut auditPort = mock(AuditLogPortOut.class);
        AccountDeletionArchiveUseCaseImpl useCase = new AccountDeletionArchiveUseCaseImpl(
                archivePort, fingerprintPort, currentAccount, auditPort);
        UUID actorId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(currentAccount.getCurrentAccountIdOrThrow()).thenReturn(actorId);
        when(archivePort.findDeletedAccount(accountId)).thenReturn(Optional.of(
                new DeletedAccountArchiveResult(accountId, AccountStatus.DELETED, requestId,
                        AccountDeletionRequestStatus.COMPLETED, Instant.now(), Instant.now(), Instant.now())));

        useCase.getDeletedAccount(accountId, "Điều tra yêu cầu hỗ trợ");

        verify(currentAccount).requirePermission("DELETED_ACCOUNT_READ_ALL");
        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditPort).save(audit.capture());
        org.junit.jupiter.api.Assertions.assertEquals(actorId, audit.getValue().getActorAccountId());
        org.junit.jupiter.api.Assertions.assertEquals(accountId, audit.getValue().getTargetId());
        org.junit.jupiter.api.Assertions.assertEquals(
                "Điều tra yêu cầu hỗ trợ", audit.getValue().getNewData().get("reason"));
    }

    @Test
    void rejectsArchiveReadWithoutReason() {
        AccountDeletionArchivePortOut archivePort = mock(AccountDeletionArchivePortOut.class);
        DeletedIdentityFingerprintPortOut fingerprintPort = mock(DeletedIdentityFingerprintPortOut.class);
        CurrentAccountPortIn currentAccount = mock(CurrentAccountPortIn.class);
        AuditLogPortOut auditPort = mock(AuditLogPortOut.class);
        AccountDeletionArchiveUseCaseImpl useCase = new AccountDeletionArchiveUseCaseImpl(
                archivePort, fingerprintPort, currentAccount, auditPort);

        assertThrows(IllegalArgumentException.class,
                () -> useCase.getDeletedAccounts(10, " "));
        verify(currentAccount).requirePermission("DELETED_ACCOUNT_READ_ALL");
    }
}
