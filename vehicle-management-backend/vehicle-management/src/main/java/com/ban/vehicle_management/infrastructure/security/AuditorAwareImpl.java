package com.ban.vehicle_management.infrastructure.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .flatMap(this::extractAccountId);
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
}
