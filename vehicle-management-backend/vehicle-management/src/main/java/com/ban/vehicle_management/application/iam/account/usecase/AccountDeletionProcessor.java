package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AccountDeletionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountDeletionProcessor.class);
    private final AccountDeletionPortOut persistence;
    private final IdentityProviderAdminPortOut identityProvider;
    private final AccountDeletionTimeProperties properties;
    private final Clock clock;

    public AccountDeletionProcessor(AccountDeletionPortOut persistence,
            IdentityProviderAdminPortOut identityProvider,
            AccountDeletionTimeProperties properties,
            Clock clock) {
        this.persistence = persistence;
        this.identityProvider = identityProvider;
        this.properties = properties;
        this.clock = clock;
    }

    public void process(UUID requestId, String claimToken) {
        AccountDeletionRequest request = persistence.findClaimed(requestId, claimToken).orElse(null);
        if (request == null) return;
        Instant now = Instant.now(clock);
        try {
            List<String> blockers = persistence.findDeletionBlockers(request.accountId());
            if (!blockers.isEmpty()) {
                persistence.fail(requestId, now, null, 1, "PRECONDITION_CHANGED", String.join(",", blockers));
                return;
            }
            String keycloakUserId = persistence.findKeycloakUserId(request.accountId());
            if (keycloakUserId != null && !keycloakUserId.isBlank()
                    && !keycloakUserId.startsWith("_deleted_")) {
                identityProvider.deleteUser(keycloakUserId);
            }
            persistence.completeWithTombstones(requestId, request.accountId(), now);
        } catch (RuntimeException exception) {
            long exponent = Math.min(request.attemptCount(), 8);
            Duration retryDelay = Duration.ofMinutes(1L << exponent);
            persistence.fail(requestId, now, now.plus(retryDelay), properties.getMaxAttempts(),
                    "PROCESSING_FAILED", exception.getMessage());
            LOGGER.error("Account deletion processing failed for requestId={}", requestId, exception);
        }
    }
}
