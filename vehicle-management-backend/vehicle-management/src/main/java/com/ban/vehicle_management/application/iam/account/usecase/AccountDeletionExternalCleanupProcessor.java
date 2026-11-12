package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionCleanupPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionCleanupItem;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountDeletionExternalCleanupProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AccountDeletionExternalCleanupProcessor.class);
    private final AccountDeletionCleanupPortOut cleanupPort;
    private final IdentityProviderAdminPortOut identityProvider;
    private final FileStoragePort fileStorage;
    private final AccountDeletionTimeProperties properties;

    public AccountDeletionExternalCleanupProcessor(
            AccountDeletionCleanupPortOut cleanupPort,
            IdentityProviderAdminPortOut identityProvider,
            FileStoragePort fileStorage,
            AccountDeletionTimeProperties properties) {
        this.cleanupPort = cleanupPort;
        this.identityProvider = identityProvider;
        this.fileStorage = fileStorage;
        this.properties = properties;
    }

    public void processForRequest(UUID requestId, Instant now) {
        String claimToken = UUID.randomUUID().toString();
        List<UUID> claimed = cleanupPort.claimDueForRequest(
                requestId, now, now.plus(properties.getProcessingLease()), claimToken,
                properties.getBatchSize());
        for (UUID cleanupItemId : claimed) {
            processClaimed(cleanupItemId, claimToken, now, true);
        }
        if (cleanupPort.hasUnfinishedDueItems(requestId, now)) {
            throw new IllegalStateException("External account cleanup is not complete");
        }
    }

    public void processCompletedRequests(Instant now) {
        String claimToken = UUID.randomUUID().toString();
        List<UUID> claimed = cleanupPort.claimDueForCompletedRequests(
                now, now.plus(properties.getProcessingLease()), claimToken,
                properties.getBatchSize());
        claimed.forEach(cleanupItemId -> processClaimed(cleanupItemId, claimToken, now, false));
    }

    private void processClaimed(UUID cleanupItemId, String claimToken, Instant now,
            boolean propagateFailure) {
        AccountDeletionCleanupItem item = cleanupPort.findClaimed(cleanupItemId, claimToken)
                .orElse(null);
        if (item == null) return;
        try {
            switch (item.resourceType()) {
                case IDENTITY_PROVIDER_USER -> identityProvider.deleteUser(item.externalIdentifier());
                case AVATAR_OBJECT, TENANT_PRIVATE_OBJECT -> fileStorage.delete(item.externalIdentifier());
            }
            cleanupPort.complete(cleanupItemId, now);
        } catch (RuntimeException exception) {
            long exponent = Math.min(item.attemptCount(), 8);
            Duration delay = properties.getRetryBaseDelay().multipliedBy(1L << exponent);
            String message = safeMessage(exception);
            cleanupPort.fail(cleanupItemId, now, now.plus(delay), properties.getMaxAttempts(), message);
            LOGGER.error("Account deletion external cleanup failed for cleanupItemId={}",
                    cleanupItemId, exception);
            if (propagateFailure) throw exception;
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
