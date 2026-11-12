package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface AccountDeletionPortOut {
    AccountStatus lockAccount(UUID accountId);
    boolean isEligibleAccount(UUID accountId);
    boolean isDeletedAccount(UUID accountId);
    Optional<AccountDeletionRequest> findActive(UUID accountId, boolean forUpdate);
    AccountDeletionRequest create(UUID accountId, AccountStatus previousStatus, Instant requestedAt, Instant scheduledAt,
            boolean activeSubscriptionTerminationWithoutRefundConfirmed);
    void markPendingDeletion(UUID accountId);
    AccountDeletionRequest cancel(UUID requestId, Instant cancelledAt, String reason);
    List<UUID> claimDue(Instant now, Instant leaseUntil, String claimToken, int batchSize);
    Optional<AccountDeletionRequest> findClaimed(UUID requestId, String claimToken);
    String findKeycloakUserId(UUID accountId);
    String findIdentityProviderSubjectSnapshot(UUID requestId);
    Optional<UUID> findDeletionCustomerId(UUID accountId);
    List<AccountDeletionBlocker> findDeletionBlockers(UUID accountId);
    List<AccountDeletionBlocker> findDeletionBlockers(UUID accountId, Collection<UUID> closingOrganizationIds);
    List<String> completeWithTombstones(UUID requestId, UUID accountId, Instant completedAt);
    void completeFileCleanup(UUID requestId, UUID accountId, Instant completedAt);
    void fail(UUID requestId, Instant now, Instant nextRetryAt, int maxAttempts, String code, String message);
    void block(UUID requestId, Instant now, String code, String message);
}
