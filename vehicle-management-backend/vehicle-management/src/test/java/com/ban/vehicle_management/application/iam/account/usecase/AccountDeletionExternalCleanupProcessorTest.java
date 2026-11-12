package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionCleanupPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionCleanupItem;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountDeletionExternalCleanupProcessorTest {

    private static final Instant NOW = Instant.parse("2026-11-20T00:00:00Z");
    private final UUID requestId = UUID.randomUUID();
    private final UUID cleanupItemId = UUID.randomUUID();
    private AccountDeletionCleanupPortOut cleanupPort;
    private IdentityProviderAdminPortOut identityProvider;
    private FileStoragePort fileStorage;
    private AccountDeletionExternalCleanupProcessor processor;

    @BeforeEach
    void setUp() {
        cleanupPort = mock(AccountDeletionCleanupPortOut.class);
        identityProvider = mock(IdentityProviderAdminPortOut.class);
        fileStorage = mock(FileStoragePort.class);
        AccountDeletionTimeProperties properties = new AccountDeletionTimeProperties();
        properties.setMaxAttempts(3);
        processor = new AccountDeletionExternalCleanupProcessor(
                cleanupPort, identityProvider, fileStorage, properties);
    }

    @Test
    void deletesClaimedIdentityAndCompletesDurableItem() {
        when(cleanupPort.claimDueForRequest(eq(requestId), eq(NOW), any(), any(), anyInt()))
                .thenReturn(List.of(cleanupItemId));
        when(cleanupPort.findClaimed(eq(cleanupItemId), any()))
                .thenReturn(Optional.of(new AccountDeletionCleanupItem(
                        cleanupItemId, requestId,
                        AccountDeletionCleanupResourceType.IDENTITY_PROVIDER_USER,
                        "keycloak-subject", 1)));

        processor.processForRequest(requestId, NOW);

        verify(identityProvider).deleteUser("keycloak-subject");
        verify(cleanupPort).complete(cleanupItemId, NOW);
    }

    @Test
    void recordsRetryBeforePropagatingObjectStorageFailure() {
        when(cleanupPort.claimDueForRequest(eq(requestId), eq(NOW), any(), any(), anyInt()))
                .thenReturn(List.of(cleanupItemId));
        when(cleanupPort.findClaimed(eq(cleanupItemId), any()))
                .thenReturn(Optional.of(new AccountDeletionCleanupItem(
                        cleanupItemId, requestId,
                        AccountDeletionCleanupResourceType.AVATAR_OBJECT,
                        "avatar/object", 1)));
        doThrow(new IllegalStateException("storage unavailable"))
                .when(fileStorage).delete("avatar/object");

        assertThrows(IllegalStateException.class,
                () -> processor.processForRequest(requestId, NOW));

        verify(cleanupPort).fail(eq(cleanupItemId), eq(NOW), any(), eq(3),
                eq("storage unavailable"));
    }
}
