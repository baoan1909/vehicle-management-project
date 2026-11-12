package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionAuditResult;
import com.ban.vehicle_management.application.iam.account.model.result.ClosedOrganizationArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedAccountArchiveResult;
import com.ban.vehicle_management.application.iam.account.model.result.DeletedIdentityMatch;
import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import java.util.List;
import java.util.UUID;

public interface AccountDeletionArchivePortIn {
    List<DeletedAccountArchiveResult> getDeletedAccounts(int limit, String reason);
    DeletedAccountArchiveResult getDeletedAccount(UUID accountId, String reason);
    List<ClosedOrganizationArchiveResult> getClosedOrganizations(int limit, String reason);
    ClosedOrganizationArchiveResult getClosedOrganization(UUID organizationId, String reason);
    AccountDeletionAuditResult getDeletionAudit(UUID requestId, String reason);
    List<DeletedIdentityMatch> lookupDeletedIdentity(
            DeletedIdentityType identityType, String value, String reason);
}
