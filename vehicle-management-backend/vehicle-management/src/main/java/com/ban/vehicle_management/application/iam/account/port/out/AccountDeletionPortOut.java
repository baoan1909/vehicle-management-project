package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountDeletionPortOut {
    void lockAccount(UUID accountId);
    boolean isActiveCustomerAccount(UUID accountId);
    boolean isDeletedAccount(UUID accountId);
    Optional<AccountDeletionRequest> findActive(UUID accountId, boolean forUpdate);
    AccountDeletionRequest create(UUID accountId, Instant requestedAt, Instant scheduledAt);
    AccountDeletionRequest cancel(UUID requestId, Instant cancelledAt, String reason);
    List<UUID> claimDue(Instant now, Instant leaseUntil, String claimToken, int batchSize);
    Optional<AccountDeletionRequest> findClaimed(UUID requestId, String claimToken);
    String findKeycloakUserId(UUID accountId);
    List<String> findDeletionBlockers(UUID accountId);
    void completeWithTombstones(UUID requestId, UUID accountId, Instant completedAt);
    void fail(UUID requestId, Instant now, Instant nextRetryAt, int maxAttempts, String code, String message);
}
