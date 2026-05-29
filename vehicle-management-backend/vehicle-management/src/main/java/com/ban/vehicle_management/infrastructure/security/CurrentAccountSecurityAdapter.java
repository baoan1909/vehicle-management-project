package com.ban.vehicle_management.infrastructure.security;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPort;
import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class CurrentAccountSecurityAdapter implements CurrentAccountPort {

    private final AccountAuthorizationPort accountAuthorizationPort;

    public CurrentAccountSecurityAdapter(AccountAuthorizationPort accountAuthorizationPort) {
        this.accountAuthorizationPort = accountAuthorizationPort;
    }

    @Override
    public Optional<CurrentAccountAccess> getCurrentAccount() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .flatMap(this::resolveCurrentAccount);
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
                .filter(account -> AccountStatus.ACTIVE.equals(account.status()))
                .map(CurrentAccountAccess::permissionCodes)
                .orElseGet(Set::of)
                .contains(normalizedPermissionCode);
    }

    @Override
    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private Optional<CurrentAccountAccess> resolveCurrentAccount(Authentication authentication) {
        Optional<UUID> accountId = resolveAccountId(authentication);
        if (accountId.isPresent()) {
            return accountAuthorizationPort.findByAccountId(accountId.get());
        }

        return resolveKeycloakUserId(authentication)
                .flatMap(accountAuthorizationPort::findByKeycloakUserId);
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
}
