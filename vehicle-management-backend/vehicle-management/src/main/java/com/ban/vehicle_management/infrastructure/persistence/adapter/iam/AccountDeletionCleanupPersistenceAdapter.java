package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionCleanupPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionCleanupItem;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionCleanupItemEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionCleanupItemRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountDeletionCleanupPersistenceAdapter implements AccountDeletionCleanupPortOut {

    private final AccountDeletionCleanupItemRepository repository;

    public AccountDeletionCleanupPersistenceAdapter(AccountDeletionCleanupItemRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void schedule(UUID requestId, UUID organizationId,
            AccountDeletionCleanupResourceType resourceType,
            String externalIdentifier, Instant scheduledAt) {
        if (externalIdentifier == null || externalIdentifier.isBlank()) {
            return;
        }
        repository.scheduleIfAbsent(requestId, organizationId, resourceType.name(),
                externalIdentifier, scheduledAt);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> claimDueForRequest(UUID requestId, Instant now, Instant leaseUntil,
            String claimToken, int batchSize) {
        return repository.claimDueForRequest(requestId, now, leaseUntil, claimToken, batchSize);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> claimDueForCompletedRequests(Instant now, Instant leaseUntil,
            String claimToken, int batchSize) {
        return repository.claimDueForCompletedRequests(now, leaseUntil, claimToken, batchSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountDeletionCleanupItem> findClaimed(UUID cleanupItemId, String claimToken) {
        return repository.findByCleanupItemIdAndClaimToken(cleanupItemId, claimToken)
                .map(entity -> new AccountDeletionCleanupItem(
                        entity.getCleanupItemId(), entity.getRequestId(), entity.getResourceType(),
                        entity.getExternalIdentifier(), entity.getAttemptCount()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUnfinishedDueItems(UUID requestId, Instant now) {
        return repository.existsByRequestIdAndScheduledAtLessThanEqualAndStatusNot(
                requestId, now, AccountDeletionCleanupStatus.COMPLETED);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID cleanupItemId, Instant completedAt) {
        repository.findById(cleanupItemId).ifPresent(entity -> {
            entity.setStatus(AccountDeletionCleanupStatus.COMPLETED);
            entity.setCompletedAt(completedAt);
            entity.setClaimToken(null);
            entity.setProcessingLeaseUntil(null);
            entity.setNextRetryAt(null);
            entity.setLastError(null);
            repository.save(entity);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID cleanupItemId, Instant now, Instant nextRetryAt,
            int maxAttempts, String message) {
        repository.findById(cleanupItemId).ifPresent(entity -> {
            boolean exhausted = entity.getAttemptCount() >= maxAttempts;
            entity.setStatus(exhausted
                    ? AccountDeletionCleanupStatus.REVIEW_REQUIRED
                    : AccountDeletionCleanupStatus.RETRY);
            entity.setNextRetryAt(exhausted ? null : nextRetryAt);
            entity.setClaimToken(null);
            entity.setProcessingLeaseUntil(null);
            entity.setLastError(message);
            repository.save(entity);
        });
    }
}
