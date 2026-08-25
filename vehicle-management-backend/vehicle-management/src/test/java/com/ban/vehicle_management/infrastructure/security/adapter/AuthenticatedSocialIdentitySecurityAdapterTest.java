package com.ban.vehicle_management.infrastructure.security.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.iam.account.model.security.AuthenticatedSocialIdentity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticatedSocialIdentitySecurityAdapterTest {

    private final AuthenticatedSocialIdentitySecurityAdapter adapter =
            new AuthenticatedSocialIdentitySecurityAdapter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReadTrustedSocialClaimsFromAuthenticatedJwt() {
        Instant now = Instant.now();
        Jwt jwt = new Jwt(
                "token",
                now,
                now.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "kc-user-1",
                        "identity_provider", "google",
                        "email", "customer@example.com",
                        "email_verified", true,
                        "preferred_username", "customer@example.com",
                        "name", "Customer One"
                )
        );
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(), "customer@example.com")
        );

        AuthenticatedSocialIdentity identity = adapter.getAuthenticatedIdentityOrThrow();

        assertEquals("kc-user-1", identity.keycloakUserId());
        assertEquals("google", identity.providerAlias());
        assertEquals("customer@example.com", identity.email());
        assertTrue(identity.emailVerified());
    }

    @Test
    void shouldRejectMissingJwtAuthentication() {
        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                adapter::getAuthenticatedIdentityOrThrow
        );
    }
}
