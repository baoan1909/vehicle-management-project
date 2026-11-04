package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.AccountDeletionPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.security.config.AccountDeletionTimeProperties;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
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

    public AccountDeletionUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            AccountDeletionPortOut persistence,
            IdentityProviderAdminPortOut identityProvider,
            AccountDeletionTimeProperties properties,
            Clock clock
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.persistence = persistence;
        this.identityProvider = identityProvider;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountDeletionRequest requestCurrentCustomerDeletion() {
        currentAccountPortIn.requirePermission(REQUEST_SELF);
        CurrentAccountAccess current = currentAccountPortIn.getCurrentAccountOrThrow();
        persistence.lockAccount(current.accountId());
        if (!persistence.isActiveCustomerAccount(current.accountId())) {
            throw new ConflictException("Tài khoản hiện tại không có Customer aggregate đủ điều kiện để xóa");
        }
        if (persistence.findActive(current.accountId(), true).isPresent()) {
            throw new ConflictException("Đã tồn tại yêu cầu xóa tài khoản đang hoạt động");
        }
        List<String> blockers = persistence.findDeletionBlockers(current.accountId());
        if (!blockers.isEmpty()) {
            throw new ConflictException("Không thể xóa tài khoản do còn nghĩa vụ chưa hoàn tất: " + String.join(", ", blockers));
        }
        Instant requestedAt = Instant.now(clock);
        AccountDeletionRequest request = persistence.create(
                current.accountId(), requestedAt, requestedAt.plus(properties.getGracePeriod()));
        if (current.subject() != null && !current.subject().isBlank()) {
            TransactionalEvents.runAfterCommit(() -> logoutSessionsSafely(current.subject()));
        }
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
        return persistence.cancel(request.requestId(), now, normalizeReason(reason, "USER_CANCELLED"));
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
        return Optional.of(persistence.cancel(request.requestId(), now, "FRESH_LOGIN_RECOVERY"));
    }

    private String normalizeReason(String reason, String fallback) {
        if (reason == null || reason.isBlank()) return fallback;
        String trimmed = reason.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), 500));
    }

    private void logoutSessionsSafely(String keycloakUserId) {
        try {
            identityProvider.logoutUserSessions(keycloakUserId);
        } catch (RuntimeException exception) {
            // The persisted pending state and lifecycle filter still block all stale sessions.
            LOGGER.error("Could not revoke Keycloak sessions after account deletion request", exception);
        }
    }
}
