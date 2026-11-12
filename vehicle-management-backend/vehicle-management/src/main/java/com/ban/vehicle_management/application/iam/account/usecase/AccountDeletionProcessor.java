package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.OrganizationClosurePortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
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
    private final AccountDeletionTimeProperties properties;
    private final Clock clock;
    private final OrganizationClosurePortOut organizationClosurePort;
    private final AccountDeletionCompletionService completionService;
    private final AccountDeletionExternalCleanupProcessor cleanupProcessor;
    private final CustomerAccountDeletionImpactService customerImpactService;
    private final CustomerAccountDeletionFinalizer customerFinalizer;

    public AccountDeletionProcessor(AccountDeletionPortOut persistence,
            AccountDeletionTimeProperties properties,
            Clock clock,
            OrganizationClosurePortOut organizationClosurePort,
            AccountDeletionCompletionService completionService,
            AccountDeletionExternalCleanupProcessor cleanupProcessor,
            CustomerAccountDeletionImpactService customerImpactService,
            CustomerAccountDeletionFinalizer customerFinalizer) {
        this.persistence = persistence;
        this.properties = properties;
        this.clock = clock;
        this.organizationClosurePort = organizationClosurePort;
        this.completionService = completionService;
        this.cleanupProcessor = cleanupProcessor;
        this.customerImpactService = customerImpactService;
        this.customerFinalizer = customerFinalizer;
    }

    public void process(UUID requestId, String claimToken) {
        AccountDeletionRequest request = persistence.findClaimed(requestId, claimToken).orElse(null);
        if (request == null) return;
        Instant now = Instant.now(clock);
        try {
            List<UUID> organizationIds = organizationClosurePort.findOrganizationIds(requestId);
            List<AccountDeletionBlocker> blockers = new java.util.ArrayList<>(
                    persistence.findDeletionBlockers(request.accountId(), organizationIds));
            var customerDecision = customerImpactService.analyze(request.accountId(), now);
            blockers.addAll(customerDecision.blockers());
            blockers.addAll(organizationClosurePort.findBlockers(organizationIds));
            if (!blockers.isEmpty()) {
                String blockerCodes = blockers.stream().map(AccountDeletionBlocker::code).distinct()
                        .collect(java.util.stream.Collectors.joining(","));
                organizationClosurePort.cancel(requestId, now, true);
                persistence.block(requestId, now, "PRECONDITION_CHANGED", blockerCodes);
                return;
            }
            if (customerDecision.automaticActions().stream()
                    .anyMatch(action -> "TERMINATE_ACTIVE_SUBSCRIPTION".equals(action.code()))) {
                customerFinalizer.terminateActiveSubscriptions(request, now);
                customerDecision = customerImpactService.analyze(request.accountId(), now);
            }
            if (!customerDecision.automaticActions().isEmpty()) {
                String actionCodes = customerDecision.automaticActions().stream()
                        .map(com.ban.vehicle_management.domain.iam.account.model.AccountDeletionAutomaticAction::code)
                        .distinct().collect(java.util.stream.Collectors.joining(","));
                organizationClosurePort.cancel(requestId, now, true);
                persistence.block(requestId, now, "AUTOMATIC_CLEANUP_REQUIRED", actionCodes);
                return;
            }
            completionService.completeDatabase(requestId, request.accountId(), now);
            cleanupProcessor.processForRequest(requestId, now);
            persistence.completeFileCleanup(requestId, request.accountId(), now);
        } catch (RuntimeException exception) {
            long exponent = Math.min(request.attemptCount(), 8);
            Duration retryDelay = properties.getRetryBaseDelay().multipliedBy(1L << exponent);
            persistence.fail(requestId, now, now.plus(retryDelay), properties.getMaxAttempts(),
                    "PROCESSING_FAILED", safeMessage(exception));
            LOGGER.error("Account deletion processing failed for requestId={}", requestId, exception);
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
