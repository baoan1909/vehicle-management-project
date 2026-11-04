package com.ban.vehicle_management.application.iam.account.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
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
    private IdentityProviderAdminPortOut identityProvider;
    private AccountDeletionProcessor processor;

    @BeforeEach
    void setUp() {
        persistence = mock(AccountDeletionPortOut.class);
        identityProvider = mock(IdentityProviderAdminPortOut.class);
        AccountDeletionTimeProperties properties = new AccountDeletionTimeProperties();
        properties.setMaxAttempts(3);
        processor = new AccountDeletionProcessor(persistence, identityProvider, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(persistence.findClaimed(requestId, claimToken)).thenReturn(Optional.of(new AccountDeletionRequest(
                requestId, accountId, AccountDeletionRequestStatus.PROCESSING,
                NOW.minusSeconds(1_209_600), NOW.minusSeconds(1), null, null, 1, null)));
    }

    @Test
    void deletesExternalIdentityBeforeReleasingLocalIdentity() {
        when(persistence.findDeletionBlockers(accountId)).thenReturn(List.of());
        when(persistence.findKeycloakUserId(accountId)).thenReturn("keycloak-user-id");

        processor.process(requestId, claimToken);

        var ordered = org.mockito.Mockito.inOrder(identityProvider, persistence);
        ordered.verify(identityProvider).deleteUser("keycloak-user-id");
        ordered.verify(persistence).completeWithTombstones(requestId, accountId, NOW);
    }

    @Test
    void changedEligibilityMovesRequestToReviewWithoutDeletingIdentity() {
        when(persistence.findDeletionBlockers(accountId)).thenReturn(List.of("UNPAID_INVOICE"));

        processor.process(requestId, claimToken);

        verify(identityProvider, never()).deleteUser(any());
        verify(persistence).fail(requestId, NOW, null, 1,
                "PRECONDITION_CHANGED", "UNPAID_INVOICE");
        verify(persistence, never()).completeWithTombstones(any(), any(), any());
    }

    @Test
    void keycloakFailureRetriesWithoutReleasingLocalIdentity() {
        when(persistence.findDeletionBlockers(accountId)).thenReturn(List.of());
        when(persistence.findKeycloakUserId(accountId)).thenReturn("keycloak-user-id");
        doThrow(new IllegalStateException("Keycloak unavailable"))
                .when(identityProvider).deleteUser("keycloak-user-id");

        processor.process(requestId, claimToken);

        verify(persistence, never()).completeWithTombstones(any(), any(), any());
        verify(persistence).fail(eq(requestId), eq(NOW), any(Instant.class), eq(3),
                eq("PROCESSING_FAILED"), eq("Keycloak unavailable"));
    }
}
