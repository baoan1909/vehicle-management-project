package com.ban.vehicle_management.application.iam.account.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.OrganizationClosurePortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionAutomaticAction;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.model.CustomerDeletionResourceDecision;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountDeletionProcessorTest {

    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");
    private final UUID requestId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final String claimToken = UUID.randomUUID().toString();
    private AccountDeletionPortOut persistence;
    private AccountDeletionProcessor processor;
    private OrganizationClosurePortOut organizationClosure;
    private AccountDeletionCompletionService completionService;
    private AccountDeletionExternalCleanupProcessor cleanupProcessor;
    private CustomerAccountDeletionImpactService customerImpactService;
    private CustomerAccountDeletionFinalizer customerFinalizer;

    @BeforeEach
    void setUp() {
        persistence = mock(AccountDeletionPortOut.class);
        organizationClosure = mock(OrganizationClosurePortOut.class);
        completionService = mock(AccountDeletionCompletionService.class);
        cleanupProcessor = mock(AccountDeletionExternalCleanupProcessor.class);
        customerImpactService = mock(CustomerAccountDeletionImpactService.class);
        customerFinalizer = mock(CustomerAccountDeletionFinalizer.class);
        AccountDeletionTimeProperties properties = new AccountDeletionTimeProperties();
        properties.setMaxAttempts(3);
        processor = new AccountDeletionProcessor(persistence, properties,
                Clock.fixed(NOW, ZoneOffset.UTC), organizationClosure, completionService,
                cleanupProcessor, customerImpactService, customerFinalizer);
        when(customerImpactService.analyze(eq(accountId), any())).thenReturn(
                new com.ban.vehicle_management.domain.iam.account.model.CustomerDeletionResourceDecision(
                        List.of(), List.of()));
        when(persistence.findClaimed(requestId, claimToken)).thenReturn(Optional.of(new AccountDeletionRequest(
                requestId, accountId, AccountDeletionRequestStatus.PROCESSING,
                NOW.minusSeconds(1_209_600), NOW.minusSeconds(1), null, null, 1, null,
                AccountStatus.ACTIVE)));
    }

    @Test
    void commitsDatabaseBeforeProcessingDurableExternalCleanup() {
        when(organizationClosure.findOrganizationIds(requestId)).thenReturn(List.of());
        when(persistence.findDeletionBlockers(accountId, List.of())).thenReturn(List.of());
        when(completionService.completeDatabase(requestId, accountId, NOW)).thenReturn(List.of("avatar-object-key"));

        processor.process(requestId, claimToken);

        var ordered = org.mockito.Mockito.inOrder(completionService, cleanupProcessor, persistence);
        ordered.verify(completionService).completeDatabase(requestId, accountId, NOW);
        ordered.verify(cleanupProcessor).processForRequest(requestId, NOW);
        ordered.verify(persistence).completeFileCleanup(requestId, accountId, NOW);
    }

    @Test
    void changedEligibilityMovesRequestToReviewWithoutDeletingIdentity() {
        when(organizationClosure.findOrganizationIds(requestId)).thenReturn(List.of());
        when(persistence.findDeletionBlockers(accountId, List.of())).thenReturn(List.of(AccountDeletionBlocker.of(
                "UNPAID_INVOICE", "Cần thanh toán hóa đơn.", "INVOICE", UUID.randomUUID())));

        processor.process(requestId, claimToken);

        verify(cleanupProcessor, never()).processForRequest(any(), any());
        verify(organizationClosure).cancel(requestId, NOW, true);
        verify(persistence).block(requestId, NOW, "PRECONDITION_CHANGED", "UNPAID_INVOICE");
        verify(completionService, never()).completeDatabase(any(), any(), any());
    }

    @Test
    void activeSubscriptionIsTerminatedOnlyWhenDueRequestIsProcessed() {
        when(organizationClosure.findOrganizationIds(requestId)).thenReturn(List.of());
        when(persistence.findDeletionBlockers(accountId, List.of())).thenReturn(List.of());
        AccountDeletionAutomaticAction terminateAction = new AccountDeletionAutomaticAction(
                "TERMINATE_ACTIVE_SUBSCRIPTION",
                "Chấm dứt vé đang hoạt động và không hoàn tiền.",
                "SUBSCRIPTION",
                UUID.randomUUID(),
                "TERMINATE_WITHOUT_REFUND",
                NOW,
                java.math.BigDecimal.ZERO,
                null);
        when(customerImpactService.analyze(accountId, NOW)).thenReturn(
                new CustomerDeletionResourceDecision(List.of(), List.of(terminateAction)),
                new CustomerDeletionResourceDecision(List.of(), List.of()));

        processor.process(requestId, claimToken);

        verify(customerFinalizer).terminateActiveSubscriptions(any(AccountDeletionRequest.class), eq(NOW));
        verify(completionService).completeDatabase(requestId, accountId, NOW);
    }

    @Test
    void externalCleanupFailureRetriesAfterDatabaseCompletion() {
        when(organizationClosure.findOrganizationIds(requestId)).thenReturn(List.of());
        when(persistence.findDeletionBlockers(accountId, List.of())).thenReturn(List.of());
        when(completionService.completeDatabase(requestId, accountId, NOW)).thenReturn(List.of());
        doThrow(new IllegalStateException("Keycloak unavailable"))
                .when(cleanupProcessor).processForRequest(requestId, NOW);

        processor.process(requestId, claimToken);

        verify(completionService).completeDatabase(requestId, accountId, NOW);
        verify(persistence).fail(eq(requestId), eq(NOW), any(Instant.class), eq(3),
                eq("PROCESSING_FAILED"), eq("Keycloak unavailable"));
    }

    @Test
    void fileCleanupFailureRetriesWithoutRollingBackLocalTombstone() {
        when(organizationClosure.findOrganizationIds(requestId)).thenReturn(List.of());
        when(persistence.findDeletionBlockers(accountId, List.of())).thenReturn(List.of());
        when(completionService.completeDatabase(requestId, accountId, NOW)).thenReturn(List.of("avatar-object-key"));
        doThrow(new IllegalStateException("Object storage unavailable"))
                .when(cleanupProcessor).processForRequest(requestId, NOW);

        processor.process(requestId, claimToken);

        verify(completionService).completeDatabase(requestId, accountId, NOW);
        verify(persistence, never()).completeFileCleanup(any(), any(), any());
        verify(persistence).fail(eq(requestId), eq(NOW), any(Instant.class), eq(3),
                eq("PROCESSING_FAILED"), eq("Object storage unavailable"));
    }
}
