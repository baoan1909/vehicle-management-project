package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionAuditResult;
import com.ban.vehicle_management.application.iam.account.model.result.ClosedOrganizationArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedAccountArchiveResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountDeletionArchivePortOut {
    List<DeletedAccountArchiveResult> findDeletedAccounts(int limit);
    Optional<DeletedAccountArchiveResult> findDeletedAccount(UUID accountId);
    List<ClosedOrganizationArchiveResult> findClosedOrganizations(int limit);
    Optional<ClosedOrganizationArchiveResult> findClosedOrganization(UUID organizationId);
    Optional<AccountDeletionAuditResult> findDeletionAudit(UUID requestId);
}
