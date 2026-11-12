package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.OrganizationClosurePortOut;
import com.ban.vehicle_management.application.iam.account.port.out.AccountSessionTerminationPortOut;
import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionView;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionImpact;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionAutomaticAction;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.AccountDeletionBlockedException;
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
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class AccountDeletionUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-11-04T00:00:00Z");
    private final UUID accountId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();
    private CurrentAccountPortIn currentAccount;
    private AccountDeletionPortOut persistence;
    private IdentityProviderAdminPortOut identityProvider;
    private AccountDeletionUseCaseImpl useCase;
    private AccountDeletionImpactService impactService;
    private OrganizationClosurePortOut organizationClosure;
    private AccountSessionTerminationPortOut sessionTermination;

    @BeforeEach
    void setUp() {
        currentAccount = mock(CurrentAccountPortIn.class);
        persistence = mock(AccountDeletionPortOut.class);
        identityProvider = mock(IdentityProviderAdminPortOut.class);
        impactService = mock(AccountDeletionImpactService.class);
        organizationClosure = mock(OrganizationClosurePortOut.class);
        sessionTermination = mock(AccountSessionTerminationPortOut.class);
        AccountDeletionTimeProperties properties = new AccountDeletionTimeProperties();
        properties.setGracePeriod(Duration.ofDays(14));
        useCase = new AccountDeletionUseCaseImpl(currentAccount, persistence, identityProvider,
                properties, Clock.fixed(NOW, ZoneOffset.UTC), impactService, organizationClosure,
                sessionTermination);
        lenient().when(impactService.analyze(accountId, true)).thenReturn(emptyImpact());
    }

    @Test
    void requestUsesPermissionAndCreatesGracePeriodWithoutCheckingRole() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(access.subject()).thenReturn("keycloak-user");
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.lockAccount(accountId)).thenReturn(AccountStatus.ACTIVE);
        when(persistence.isEligibleAccount(accountId)).thenReturn(true);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.empty());
        AccountDeletionRequest created = request(AccountDeletionRequestStatus.GRACE_PERIOD, NOW,
                NOW.plus(Duration.ofDays(14)));
        when(persistence.create(accountId, AccountStatus.ACTIVE, NOW, NOW.plus(Duration.ofDays(14)), false))
                .thenReturn(created);

        AccountDeletionRequest result = useCase.requestCurrentAccountDeletion();

        assertEquals(created, result);
        verify(currentAccount).requirePermission(AccountDeletionUseCaseImpl.REQUEST_SELF);
        verify(persistence).markPendingDeletion(accountId);
        verify(identityProvider).logoutUserSessions("keycloak-user");
        verify(sessionTermination).publish(accountId, requestId, NOW);
    }

    @Test
    void confirmedRequestCalledByControllerRunsInsideSpringTransactionProxy() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.lockAccount(accountId)).thenReturn(AccountStatus.ACTIVE);
        when(persistence.isEligibleAccount(accountId)).thenReturn(true);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.empty());
        AccountDeletionRequest created = request(AccountDeletionRequestStatus.GRACE_PERIOD, NOW,
                NOW.plus(Duration.ofDays(14)));
        when(persistence.create(accountId, AccountStatus.ACTIVE, NOW, NOW.plus(Duration.ofDays(14)), false))
                .thenReturn(created);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        AccountDeletionPortIn proxiedUseCase = transactionalProxy(transactionManager);

        AccountDeletionView result = proxiedUseCase.requestCurrentAccountDeletionView(true);

        assertEquals(created, result.request());
        assertEquals(1, transactionManager.beginCount);
        assertEquals(1, transactionManager.commitCount);
        assertEquals(0, transactionManager.rollbackCount);
    }

    @Test
    void cancellationCalledByControllerRunsInsideSpringTransactionProxy() {
        AccountDeletionRequest pending = request(AccountDeletionRequestStatus.GRACE_PERIOD,
                NOW.minusSeconds(60), NOW.plus(Duration.ofDays(14)));
        AccountDeletionRequest cancelled = request(AccountDeletionRequestStatus.CANCELLED,
                pending.requestedAt(), pending.scheduledDeletionAt());
        when(currentAccount.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.of(pending));
        when(persistence.cancel(requestId, NOW, "PROFILE_CANCELLED")).thenReturn(cancelled);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        AccountDeletionPortIn proxiedUseCase = transactionalProxy(transactionManager);

        AccountDeletionView result = proxiedUseCase.cancelCurrentRequestView("PROFILE_CANCELLED");

        assertEquals(cancelled, result.request());
        assertEquals(1, transactionManager.beginCount);
        assertEquals(1, transactionManager.commitCount);
        assertEquals(0, transactionManager.rollbackCount);
        verify(organizationClosure).cancel(requestId, NOW, false);
    }

    @Test
    void customerRoleWithoutPermissionIsDeniedBeforeEligibility() {
        doThrow(new AccessDeniedException("denied"))
                .when(currentAccount).requirePermission(AccountDeletionUseCaseImpl.REQUEST_SELF);

        assertThrows(AccessDeniedException.class, useCase::requestCurrentAccountDeletion);
    }

    @Test
    void permissionWithoutCustomerAggregateReturnsBusinessConflict() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.lockAccount(accountId)).thenReturn(AccountStatus.ACTIVE);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.empty());
        when(persistence.isEligibleAccount(accountId)).thenReturn(false);

        assertThrows(ConflictException.class, useCase::requestCurrentAccountDeletion);
    }

    @Test
    void duplicateActiveRequestReturnsConflict() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.lockAccount(accountId)).thenReturn(AccountStatus.ACTIVE);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.of(request(
                AccountDeletionRequestStatus.GRACE_PERIOD, NOW, NOW.plus(Duration.ofDays(14)))));

        assertThrows(ConflictException.class, useCase::requestCurrentAccountDeletion);
    }

    @Test
    void requestRejectsOutstandingBusinessObligations() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.lockAccount(accountId)).thenReturn(AccountStatus.ACTIVE);
        when(persistence.isEligibleAccount(accountId)).thenReturn(true);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.empty());
        when(impactService.analyze(accountId, true)).thenReturn(new AccountDeletionImpact(
                List.of(AccountDeletionBlocker.of(
                        "UNPAID_INVOICE", "Cần thanh toán hóa đơn.", "INVOICE", UUID.randomUUID())),
                List.of(), List.of(), 0, 0, 0, List.of(), List.of(), false));

        assertThrows(AccountDeletionBlockedException.class, useCase::requestCurrentAccountDeletion);
    }

    @Test
    void activeSubscriptionRequiresExplicitNoRefundConfirmation() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.lockAccount(accountId)).thenReturn(AccountStatus.ACTIVE);
        when(persistence.isEligibleAccount(accountId)).thenReturn(true);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.empty());
        when(impactService.analyze(accountId, true)).thenReturn(activeSubscriptionImpact());

        assertThrows(ConflictException.class,
                () -> useCase.requestCurrentAccountDeletionView(false, false));
    }

    @Test
    void activeSubscriptionConsentIsPersistedForFinalization() {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(currentAccount.getCurrentAccountOrThrow()).thenReturn(access);
        when(persistence.lockAccount(accountId)).thenReturn(AccountStatus.ACTIVE);
        when(persistence.isEligibleAccount(accountId)).thenReturn(true);
        when(persistence.findActive(accountId, true)).thenReturn(Optional.empty());
        when(impactService.analyze(accountId, true)).thenReturn(activeSubscriptionImpact());
        AccountDeletionRequest created = new AccountDeletionRequest(
                requestId, accountId, AccountDeletionRequestStatus.GRACE_PERIOD, NOW,
                NOW.plus(Duration.ofDays(14)), null, null, 0, null, AccountStatus.ACTIVE, true);
        when(persistence.create(accountId, AccountStatus.ACTIVE, NOW,
                NOW.plus(Duration.ofDays(14)), true)).thenReturn(created);

        AccountDeletionView result = useCase.requestCurrentAccountDeletionView(false, true);

        assertTrue(result.request().activeSubscriptionNoRefundConfirmed());
        verify(persistence).create(accountId, AccountStatus.ACTIVE, NOW,
                NOW.plus(Duration.ofDays(14)), true);
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
        verify(organizationClosure).cancel(requestId, NOW, false);
        verify(currentAccount).invalidateCurrentAccountSnapshot();
        verify(currentAccount).refreshCurrentAccountOrThrow();
    }

    @Test
    void staleSessionCannotAutoCancel() {
        AccountDeletionRequest pending = request(AccountDeletionRequestStatus.GRACE_PERIOD,
                NOW.minusSeconds(60), NOW.plus(Duration.ofDays(14)));
        when(persistence.findActive(accountId, true)).thenReturn(Optional.of(pending));

        assertTrue(useCase.cancelOnFreshLogin(accountId, NOW.minusSeconds(120)).isEmpty());
    }

    @Test
    void authenticationAtRequestTimeCannotAutoCancel() {
        AccountDeletionRequest pending = request(AccountDeletionRequestStatus.GRACE_PERIOD,
                NOW.minusSeconds(60), NOW.plus(Duration.ofDays(14)));
        when(persistence.findActive(accountId, true)).thenReturn(Optional.of(pending));

        assertTrue(useCase.cancelOnFreshLogin(accountId, pending.requestedAt()).isEmpty());
    }

    @Test
    void missingAuthenticationTimeCannotAutoCancel() {
        AccountDeletionRequest pending = request(AccountDeletionRequestStatus.GRACE_PERIOD,
                NOW.minusSeconds(60), NOW.plus(Duration.ofDays(14)));
        when(persistence.findActive(accountId, true)).thenReturn(Optional.of(pending));

        assertTrue(useCase.cancelOnFreshLogin(accountId, null).isEmpty());
    }

    private AccountDeletionRequest request(AccountDeletionRequestStatus status, Instant requestedAt,
            Instant scheduledAt) {
        return new AccountDeletionRequest(requestId, accountId, status, requestedAt, scheduledAt,
                status == AccountDeletionRequestStatus.CANCELLED ? NOW : null, null, 0, null,
                AccountStatus.ACTIVE);
    }

    private AccountDeletionImpact emptyImpact() {
        return new AccountDeletionImpact(List.of(), List.of(), List.of(),
                0, 0, 0, List.of(), List.of(), false);
    }

    private AccountDeletionImpact activeSubscriptionImpact() {
        return new AccountDeletionImpact(
                List.of(),
                List.of(),
                List.of(new AccountDeletionAutomaticAction(
                        "TERMINATE_ACTIVE_SUBSCRIPTION",
                        "Vé đang hoạt động sẽ bị chấm dứt và không được hoàn tiền.",
                        "SUBSCRIPTION",
                        UUID.randomUUID(),
                        "TERMINATE_WITHOUT_REFUND",
                        null,
                        java.math.BigDecimal.ZERO,
                        null)),
                List.of(),
                0, 0, 0,
                List.of(),
                List.of(),
                false);
    }

    private AccountDeletionPortIn transactionalProxy(RecordingTransactionManager transactionManager) {
        ProxyFactory proxyFactory = new ProxyFactory(useCase);
        proxyFactory.addAdvice(new TransactionInterceptor(
                transactionManager, new AnnotationTransactionAttributeSource()));
        return (AccountDeletionPortIn) proxyFactory.getProxy();
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private int beginCount;
        private int commitCount;
        private int rollbackCount;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            beginCount++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbackCount++;
        }
    }
}
