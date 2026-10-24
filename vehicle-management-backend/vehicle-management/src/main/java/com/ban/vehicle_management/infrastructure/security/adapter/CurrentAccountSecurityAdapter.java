package com.ban.vehicle_management.infrastructure.security.adapter;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.security.assistant.AssistantActorScope;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class CurrentAccountSecurityAdapter implements CurrentAccountPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentAccountSecurityAdapter.class);

    private final AccountAuthorizationPortOut accountAuthorizationPortOut;
    private final AssistantActorScope assistantActorScope;

    public CurrentAccountSecurityAdapter(
            AccountAuthorizationPortOut accountAuthorizationPortOut,
            AssistantActorScope assistantActorScope) {
        this.accountAuthorizationPortOut = accountAuthorizationPortOut;
        this.assistantActorScope = assistantActorScope;
    }

    @Override
    public Optional<CurrentAccountAccess> getCurrentAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return resolveCurrentAccount(authentication);
        }
        // Scheduler threads (assistant/ingestion workers) have no SecurityContext:
        // fall back to the job-bound actor, resolved from persisted data only.
        return assistantActorScope.currentActorAccountId()
                .flatMap(accountAuthorizationPortOut::findByAccountId);
    }

    @Override
    public CurrentAccountAccess getCurrentAccountOrThrow() {
        return getCurrentAccount()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Authenticated account could not be resolved"
                ));
    }

    @Override
    public Optional<UUID> getCurrentAccountId() {
        return getCurrentAccount().map(CurrentAccountAccess::accountId);
    }

    @Override
    public UUID getCurrentAccountIdOrThrow() {
        return getCurrentAccountOrThrow().accountId();
    }

    @Override
    public boolean hasPermission(String permissionCode) {
        String normalizedPermissionCode = normalizePermissionCode(permissionCode);

        return getCurrentAccount()
                .filter(CurrentAccountAccess::canUseBusinessPermissions)
                .map(CurrentAccountAccess::getEffectivePermissionCodes)
                .orElseGet(Set::of)
                .contains(normalizedPermissionCode);
    }

    @Override
    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    @Override
    public void invalidateCurrentAccountSnapshot() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AbstractAuthenticationToken authenticationToken)) {
            return;
        }
        if (authenticationToken.getDetails() instanceof AuthenticatedAccountPrincipal principal) {
            authenticationToken.setDetails(new AuthenticatedAccountPrincipal(
                    principal.accountId(),
                    principal.keycloakUserId(),
                    principal.preferredUsername(),
                    principal.email()
            ));
        }
    }

    @Override
    public CurrentAccountAccess refreshCurrentAccountOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return getCurrentAccountOrThrow();
        }
        invalidateCurrentAccountSnapshot();
        return loadAndEnrich(authentication)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Authenticated account could not be resolved"
                ));
    }

    private Optional<CurrentAccountAccess> resolveCurrentAccount(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof AuthenticatedAccountPrincipal principal
                && principal.accessSnapshot() != null) {
            if (!principal.hasConsistentSnapshot()
                    || !authenticationMatchesSnapshot(authentication, principal.accessSnapshot())) {
                LOGGER.warn("Rejected inconsistent current-account access snapshot");
                return Optional.empty();
            }
            return Optional.of(principal.accessSnapshot());
        }
        return loadAndEnrich(authentication);
    }

    private Optional<CurrentAccountAccess> loadAndEnrich(Authentication authentication) {
        Optional<UUID> accountId = resolveAccountId(authentication);
        if (accountId.isPresent()) {
            return accountAuthorizationPortOut.findByAccountId(accountId.get())
                    .map(account -> {
                        enrichAuthenticationDetails(authentication, account);
                        return account;
                    });
        }

        return resolveKeycloakUserId(authentication)
                .flatMap(accountAuthorizationPortOut::findByKeycloakUserId)
                .map(account -> {
                    enrichAuthenticationDetails(authentication, account);
                    return account;
                });
    }

    private Optional<UUID> resolveAccountId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof AuthenticatedAccountPrincipal principal
                && principal.accountId() != null) {
            return Optional.of(principal.accountId());
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return parseUuid(jwt.getClaim("account_id"));
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return parseUuid(jwtAuthenticationToken.getToken().getClaim("account_id"));
        }

        return Optional.empty();
    }

    private boolean authenticationMatchesSnapshot(
            Authentication authentication,
            CurrentAccountAccess snapshot
    ) {
        Optional<UUID> authenticationAccountId = resolveAccountId(authentication);
        if (authenticationAccountId.isPresent()
                && !authenticationAccountId.get().equals(snapshot.accountId())) {
            return false;
        }
        Optional<String> subject = resolveKeycloakUserId(authentication);
        return subject.isEmpty()
                || snapshot.subject() == null
                || subject.get().equals(snapshot.subject());
    }

    private Optional<String> resolveKeycloakUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof AuthenticatedAccountPrincipal principal
                && principal.keycloakUserId() != null
                && !principal.keycloakUserId().isBlank()) {
            return Optional.of(principal.keycloakUserId());
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            return subject == null || subject.isBlank() ? Optional.empty() : Optional.of(subject);
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String subject = jwtAuthenticationToken.getToken().getSubject();
            return subject == null || subject.isBlank() ? Optional.empty() : Optional.of(subject);
        }

        return Optional.empty();
    }

    private Optional<UUID> parseUuid(Object claimValue) {
        if (claimValue instanceof UUID uuid) {
            return Optional.of(uuid);
        }

        if (claimValue instanceof String text && !text.isBlank()) {
            try {
                return Optional.of(UUID.fromString(text));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private String normalizePermissionCode(String permissionCode) {
        return TextValidationUtils.normalizeCode(permissionCode, "permissionCode", 100);
    }

    private void enrichAuthenticationDetails(Authentication authentication, CurrentAccountAccess accountAccess) {
        if (!(authentication instanceof AbstractAuthenticationToken authenticationToken)) {
            return;
        }

        Object details = authenticationToken.getDetails();
        if (details instanceof AuthenticatedAccountPrincipal principal
                && accountAccess.accountId().equals(principal.accountId())
                && principal.accessSnapshot() != null) {
            return;
        }

        String preferredUsername;
        String email;
        String keycloakUserId;

        if (details instanceof AuthenticatedAccountPrincipal principal) {
            preferredUsername = principal.preferredUsername();
            email = principal.email();
            keycloakUserId = principal.keycloakUserId();
        } else {
            preferredUsername = accountAccess.username();
            email = accountAccess.email();
            keycloakUserId = accountAccess.subject();
        }

        authenticationToken.setDetails(new AuthenticatedAccountPrincipal(
                accountAccess.accountId(),
                keycloakUserId,
                preferredUsername,
                email,
                accountAccess
        ));
    }
}
