package com.ban.vehicle_management.infrastructure.security.adapter;

import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<UUID> {

    private final SystemAccountIdProvider systemAccountIdProvider;

    public AuditorAwareImpl(SystemAccountIdProvider systemAccountIdProvider) {
        this.systemAccountIdProvider = systemAccountIdProvider;
    }

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAnonymousOrUnauthenticated(authentication)) {
            return Optional.of(systemAccountIdProvider.getSystemAccountId());
        }

        Optional<UUID> accountId = extractAccountId(authentication);
        return accountId.isPresent() ? accountId : Optional.of(systemAccountIdProvider.getSystemAccountId());
    }

    private Optional<UUID> extractAccountId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof AuthenticatedAccountPrincipal principal) {
            return Optional.ofNullable(principal.accountId());
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return parseAccountId(jwt.getClaim("account_id"));
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return parseAccountId(jwtAuthenticationToken.getToken().getClaim("account_id"));
        }

        return Optional.empty();
    }

    private Optional<UUID> parseAccountId(Object claimValue) {
        if (claimValue == null) {
            return Optional.empty();
        }

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

    private boolean isAnonymousOrUnauthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        return principal instanceof String text && "anonymousUser".equalsIgnoreCase(text);
    }
}
