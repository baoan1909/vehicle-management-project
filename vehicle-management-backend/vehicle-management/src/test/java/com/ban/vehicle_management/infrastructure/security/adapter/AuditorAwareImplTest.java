package com.ban.vehicle_management.infrastructure.security.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditorAwareImplTest {

    private static final UUID SYSTEM_ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-00000000a001");

    private final AuditorAwareImpl auditorAware = new AuditorAwareImpl(() -> SYSTEM_ACCOUNT_ID);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldFallbackToSystemAccountWhenNoAuthenticatedUser() {
        UUID resolvedAuditor = auditorAware.getCurrentAuditor().orElseThrow();
        assertEquals(SYSTEM_ACCOUNT_ID, resolvedAuditor);
    }

    @Test
    void shouldFallbackToSystemWhenAuthenticationIsAnonymousToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "anonymous-key",
                        "anonymousUser",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                )
        );

        assertEquals(SYSTEM_ACCOUNT_ID, auditorAware.getCurrentAuditor().orElseThrow());
    }

    @Test
    void shouldResolveAuditorFromAuthenticatedPrincipalDetails() {
        UUID accountId = UUID.fromString("5c699173-c10f-4985-8d63-38d8ddb89f6a");

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "user",
                "pass",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticationToken.setDetails(new AuthenticatedAccountPrincipal(
                accountId,
                "kc-user-id-123",
                "baoan3236",
                "baoan3236@gmail.com"
        ));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        assertEquals(accountId, auditorAware.getCurrentAuditor().orElseThrow());
    }

    @Test
    void shouldFallbackToSystemWhenAuthenticationExistsButAccountIdIsMissing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        "pass",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        assertEquals(SYSTEM_ACCOUNT_ID, auditorAware.getCurrentAuditor().orElseThrow());
    }

    @Test
    void shouldFallbackToSystemWhenPrincipalDetailsHasNoAccountId() {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "user",
                "pass",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticationToken.setDetails(new AuthenticatedAccountPrincipal(
                null,
                "kc-user-id-123",
                "baoan3236",
                "baoan3236@gmail.com"
        ));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        assertEquals(SYSTEM_ACCOUNT_ID, auditorAware.getCurrentAuditor().orElseThrow());
    }
}
