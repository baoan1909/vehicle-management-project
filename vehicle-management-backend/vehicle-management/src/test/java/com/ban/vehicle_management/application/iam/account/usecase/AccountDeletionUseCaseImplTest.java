package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AccountDeletionUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");
    private final UUID accountId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();
    private CurrentAccountPortIn currentAccount;
    private AccountDeletionPortOut persistence;
    private IdentityProviderAdminPortOut identityProvider;
    private AccountDeletionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        currentAccount = mock(CurrentAccountPortIn.class);
        persistence = mock(AccountDeletionPortOut.class);
        identityProvider = mock(IdentityProviderAdminPortOut.class);
        AccountDeletionTimeProperties properties = new AccountDeletionTimeProperties();
        properties.setGracePeriod(Duration.ofDays(14));
        useCase = new AccountDeletionUseCaseImpl(currentAccount, persistence, identityProvider,
                properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void requestUsesPermissionAndCreatesGracePeriodWithoutCheckingRole() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(access.subject()).thenReturn("keycloak-user");
        when(access.roleCode()).thenReturn("NOT_CUSTOMER");
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.isActiveCustomerAccount(accountId)).thenReturn(true);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.empty());
        when(persistence.findDeletionBlockers(accountId)).thenReturn(List.of());
        AccountDeletionRequest created = request(AccountDeletionRequestStatus.GRACE_PERIOD, NOW,
                NOW.plus(Duration.ofDays(14)));
        when(persistence.create(accountId, NOW, NOW.plus(Duration.ofDays(14)))).thenReturn(created);

        AccountDeletionRequest result = useCase.requestCurrentCustomerDeletion();

        assertEquals(created, result);
        verify(currentAccount).requirePermission(AccountDeletionUseCaseImpl.REQUEST_SELF);
        verify(identityProvider).logoutUserSessions("keycloak-user");
    }

    @Test
    void customerRoleWithoutPermissionIsDeniedBeforeEligibility() {
        doThrow(new AccessDeniedException("denied"))
                .when(currentAccount).requirePermission(AccountDeletionUseCaseImpl.REQUEST_SELF);

        assertThrows(AccessDeniedException.class, useCase::requestCurrentCustomerDeletion);
    }

    @Test
    void permissionWithoutCustomerAggregateReturnsBusinessConflict() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.isActiveCustomerAccount(accountId)).thenReturn(false);

        assertThrows(ConflictException.class, useCase::requestCurrentCustomerDeletion);
    }

    @Test
    void duplicateActiveRequestReturnsConflict() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.isActiveCustomerAccount(accountId)).thenReturn(true);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.of(request(
                AccountDeletionRequestStatus.GRACE_PERIOD, NOW, NOW.plus(Duration.ofDays(14)))));

        assertThrows(ConflictException.class, useCase::requestCurrentCustomerDeletion);
    }

    @Test
    void requestRejectsOutstandingBusinessObligations() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.isActiveCustomerAccount(accountId)).thenReturn(true);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.empty());
        when(persistence.findDeletionBlockers(accountId)).thenReturn(List.of("UNPAID_INVOICE"));

        assertThrows(ConflictException.class, useCase::requestCurrentCustomerDeletion);
    }

    @Test
    void freshLoginAfterRequestCancelsWithinGracePeriod() {
        AccountDeletionRequest pending = request(AccountDeletionRequestStatus.GRACE_PERIOD,
                NOW.minusSeconds(60), NOW.plus(Duration.ofDays(14)));
        when(persistence.findActive(accountId, true)).thenReturn(Optional.of(pending));
        AccountDeletionRequest cancelled = request(AccountDeletionRequestStatus.CANCELLED,
                pending.requestedAt(), pending.scheduledDeletionAt());
        when(persistence.cancel(requestId, NOW, "FRESH_LOGIN_RECOVERY")).thenReturn(cancelled);

        Optional<AccountDeletionRequest> result = useCase.cancelOnFreshLogin(accountId, NOW.minusSeconds(30));

        assertTrue(result.isPresent());
        assertEquals(AccountDeletionRequestStatus.CANCELLED, result.orElseThrow().status());
    }

    @Test
    void staleSessionCannotAutoCancel() {
        AccountDeletionRequest pending = request(AccountDeletionRequestStatus.GRACE_PERIOD,
                NOW.minusSeconds(60), NOW.plus(Duration.ofDays(14)));
        when(persistence.findActive(accountId, true)).thenReturn(Optional.of(pending));

        assertTrue(useCase.cancelOnFreshLogin(accountId, NOW.minusSeconds(120)).isEmpty());
    }

    private AccountDeletionRequest request(AccountDeletionRequestStatus status, Instant requestedAt,
            Instant scheduledAt) {
        return new AccountDeletionRequest(requestId, accountId, status, requestedAt, scheduledAt,
                status == AccountDeletionRequestStatus.CANCELLED ? NOW : null, null, 0, null);
    }
}
