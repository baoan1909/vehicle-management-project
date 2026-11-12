package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionPortIn;
import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionView;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.AccountSessionTerminationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.AccountDeletionBlockedException;
import com.ban.vehicle_management.shared.exception.OrganizationClosureConfirmationRequiredException;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionImpact;
import com.ban.vehicle_management.domain.iam.account.model.OrganizationClosureImpact;
import com.ban.vehicle_management.application.iam.account.port.out.OrganizationClosurePortOut;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AccountDeletionUseCaseImpl implements AccountDeletionPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountDeletionUseCaseImpl.class);

    public static final String REQUEST_SELF = "ACCOUNT_DELETION_REQUEST_SELF";
    public static final String READ_SELF = "ACCOUNT_DELETION_READ_SELF";
    public static final String CANCEL_SELF = "ACCOUNT_DELETION_CANCEL_SELF";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountDeletionPortOut persistence;
    private final IdentityProviderAdminPortOut identityProvider;
    private final AccountDeletionTimeProperties properties;
    private final Clock clock;
    private final AccountDeletionImpactService impactService;
    private final OrganizationClosurePortOut organizationClosurePort;
    private final AccountSessionTerminationPortOut sessionTerminationPort;

    public AccountDeletionUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            AccountDeletionPortOut persistence,
            IdentityProviderAdminPortOut identityProvider,
            AccountDeletionTimeProperties properties,
            Clock clock,
            AccountDeletionImpactService impactService,
            OrganizationClosurePortOut organizationClosurePort,
            AccountSessionTerminationPortOut sessionTerminationPort
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.persistence = persistence;
        this.identityProvider = identityProvider;
        this.properties = properties;
        this.clock = clock;
        this.impactService = impactService;
        this.organizationClosurePort = organizationClosurePort;
        this.sessionTerminationPort = sessionTerminationPort;
    }

    @Override
    @Transactional
    public AccountDeletionRequest requestCurrentAccountDeletion() {
        return requestCurrentAccountDeletion(false, false);
    }

    private AccountDeletionRequest requestCurrentAccountDeletion(boolean confirmOrganizationClosure,
            boolean confirmActiveSubscriptionTerminationWithoutRefund) {
        currentAccountPortIn.requirePermission(REQUEST_SELF);
        CurrentAccountAccess current = currentAccountPortIn.getCurrentAccountOrThrow();
        AccountStatus previousStatus = persistence.lockAccount(current.accountId());
        if (persistence.findActive(current.accountId(), true).isPresent()) {
            throw new ConflictException("Đã tồn tại yêu cầu xóa tài khoản đang hoạt động");
        }
        if (previousStatus != AccountStatus.ACTIVE && previousStatus != AccountStatus.PENDING) {
            throw new ConflictException("Trạng thái tài khoản hiện tại không cho phép yêu cầu xóa");
        }
        if (!persistence.isEligibleAccount(current.accountId())) {
            throw new ConflictException("Tài khoản hiện tại không đủ điều kiện khởi tạo yêu cầu xóa");
        }
        AccountDeletionImpact impact = impactService.analyze(current.accountId(), true);
        List<AccountDeletionBlocker> blockers = impact.allBlockers();
        if (!blockers.isEmpty()) {
            throw new AccountDeletionBlockedException(blockers);
        }
        if (impact.confirmationRequired() && !confirmOrganizationClosure) {
            throw new OrganizationClosureConfirmationRequiredException(impact);
        }
        boolean terminatesActiveSubscription = impact.automaticActions().stream()
                .anyMatch(action -> "TERMINATE_ACTIVE_SUBSCRIPTION".equals(action.code()));
        if (terminatesActiveSubscription && !confirmActiveSubscriptionTerminationWithoutRefund) {
            throw new ConflictException(
                    "Bạn phải xác nhận vé đang hoạt động sẽ bị chấm dứt và không được hoàn tiền");
        }
        Instant requestedAt = Instant.now(clock);
        persistence.markPendingDeletion(current.accountId());
        AccountDeletionRequest request = persistence.create(
                current.accountId(), previousStatus, requestedAt, requestedAt.plus(properties.getGracePeriod()),
                confirmActiveSubscriptionTerminationWithoutRefund);
        organizationClosurePort.begin(request.requestId(), current.accountId(), impact.organizationClosureScopes().stream()
                .map(OrganizationClosureImpact::organizationId).toList(), requestedAt);
        TransactionalEvents.runAfterCommit(() -> {
            publishSessionTerminationSafely(current.accountId(), request.requestId(), requestedAt);
            if (current.subject() != null && !current.subject().isBlank()) {
                logoutSessionsSafely(current.subject());
            }
        });
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountDeletionRequest> getCurrentRequest() {
        currentAccountPortIn.requirePermission(READ_SELF);
        return persistence.findActive(currentAccountPortIn.getCurrentAccountIdOrThrow(), false);
    }

    @Override
    @Transactional
    public AccountDeletionRequest cancelCurrentRequest(String reason) {
        currentAccountPortIn.requirePermission(CANCEL_SELF);
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        AccountDeletionRequest request = persistence.findActive(accountId, true)
                .orElseThrow(() -> new NotFoundException("Không có yêu cầu xóa tài khoản đang hoạt động"));
        Instant now = Instant.now(clock);
        if (!request.cancellableAt(now)) {
            throw new ConflictException("Yêu cầu xóa đã hết thời hạn hủy hoặc đang được xử lý");
        }
        organizationClosurePort.cancel(request.requestId(), now, false);
        AccountDeletionRequest cancelled = persistence.cancel(
                request.requestId(), now, normalizeReason(reason, "USER_CANCELLED"));
        refreshCurrentAccountSnapshot();
        return cancelled;
    }

    @Override
    @Transactional
    public Optional<AccountDeletionRequest> cancelOnFreshLogin(UUID accountId, Instant authenticatedAt) {
        if (accountId == null) return Optional.empty();
        AccountDeletionRequest request = persistence.findActive(accountId, true).orElse(null);
        if (request == null) return Optional.empty();
        Instant now = Instant.now(clock);
        boolean freshLogin = authenticatedAt != null && authenticatedAt.isAfter(request.requestedAt());
        if (!freshLogin || !request.cancellableAt(now)) return Optional.empty();
        organizationClosurePort.cancel(request.requestId(), now, false);
        AccountDeletionRequest cancelled = persistence.cancel(
                request.requestId(), now, "FRESH_LOGIN_RECOVERY");
        refreshCurrentAccountSnapshot();
        return Optional.of(cancelled);
    }

    @Override
    @Transactional
    public AccountDeletionView requestCurrentAccountDeletionView() {
        return toView(requestCurrentAccountDeletion());
    }

    @Override
    @Transactional
    public AccountDeletionView requestCurrentAccountDeletionView(boolean confirmOrganizationClosure) {
        return toView(requestCurrentAccountDeletion(confirmOrganizationClosure, false));
    }

    @Override
    @Transactional
    public AccountDeletionView requestCurrentAccountDeletionView(boolean confirmOrganizationClosure,
            boolean confirmActiveSubscriptionTerminationWithoutRefund) {
        return toView(requestCurrentAccountDeletion(
                confirmOrganizationClosure, confirmActiveSubscriptionTerminationWithoutRefund));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDeletionImpact getCurrentAccountDeletionImpact() {
        currentAccountPortIn.requirePermission(REQUEST_SELF);
        return impactService.analyze(currentAccountPortIn.getCurrentAccountIdOrThrow(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountDeletionView> getCurrentRequestView() {
        return getCurrentRequest().map(this::toView);
    }

    @Override
    @Transactional
    public AccountDeletionView cancelCurrentRequestView(String reason) {
        return toView(cancelCurrentRequest(reason));
    }

    private AccountDeletionView toView(AccountDeletionRequest request) {
        return new AccountDeletionView(request, request.cancellableAt(Instant.now(clock)));
    }

    private String normalizeReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) return fallback;
        String trimmed = reason.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), 500));
    }

    private void refreshCurrentAccountSnapshot() {
        currentAccountPortIn.invalidateCurrentAccountSnapshot();
        currentAccountPortIn.refreshCurrentAccountOrThrow();
    }

    private void logoutSessionsSafely(String keycloakUserId) {
        try {
            identityProvider.logoutUserSessions(keycloakUserId);
        } catch (RuntimeException exception) {
            // The persisted pending state and lifecycle filter still block all stale sessions.
            LOGGER.error("Could not revoke Keycloak sessions after account deletion request", exception);
        }
    }

    private void publishSessionTerminationSafely(UUID accountId, UUID requestId, Instant occurredAt) {
        try {
            sessionTerminationPort.publish(accountId, requestId, occurredAt);
        } catch (RuntimeException exception) {
            // Keycloak revocation and the lifecycle filter remain authoritative fallbacks.
            LOGGER.error("Could not publish account deletion session termination event", exception);
        }
    }
}
