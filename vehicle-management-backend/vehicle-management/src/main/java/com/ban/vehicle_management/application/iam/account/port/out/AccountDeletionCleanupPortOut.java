package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionCleanupItem;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountDeletionCleanupPortOut {

    void schedule(UUID requestId, UUID organizationId,
            AccountDeletionCleanupResourceType resourceType,
            String externalIdentifier, Instant scheduledAt);

    List<UUID> claimDueForRequest(UUID requestId, Instant now, Instant leaseUntil,
            String claimToken, int batchSize);

    List<UUID> claimDueForCompletedRequests(Instant now, Instant leaseUntil,
            String claimToken, int batchSize);

    Optional<AccountDeletionCleanupItem> findClaimed(UUID cleanupItemId, String claimToken);

    boolean hasUnfinishedDueItems(UUID requestId, Instant now);

    void complete(UUID cleanupItemId, Instant completedAt);

    void fail(UUID cleanupItemId, Instant now, Instant nextRetryAt,
            int maxAttempts, String message);
}
