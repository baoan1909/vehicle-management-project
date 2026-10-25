package com.ban.vehicle_management.infrastructure.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAudienceValidatorTest {

    private final JwtAudienceValidator validator = new JwtAudienceValidator(
            List.of("vehicle-management-api"),
            List.of("vehicle-management-frontend")
    );

    @Test
    void shouldAcceptConfiguredAudience() {
        OAuth2TokenValidatorResult result = validator.validate(jwt(
                List.of("vehicle-management-api"),
                "untrusted-client"
        ));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldAcceptConfiguredAuthorizedPartyWhenAudienceIsNotMappedByKeycloak() {
        OAuth2TokenValidatorResult result = validator.validate(jwt(
                List.of("account"),
                "vehicle-management-frontend"
        ));

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldRejectTokenForDifferentAudienceAndClient() {
        OAuth2TokenValidatorResult result = validator.validate(jwt(
                List.of("different-api"),
                "different-client"
        ));

        assertTrue(result.hasErrors());
    }

    @Test
    void shouldAllowDefaultValidationWhenNoAudienceConstraintIsConfigured() {
        JwtAudienceValidator unconstrained = new JwtAudienceValidator(List.of(), List.of());

        assertFalse(unconstrained.validate(jwt(List.of("anything"), "anything")).hasErrors());
    }

    private Jwt jwt(List<String> audience, String authorizedParty) {
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("keycloak-user")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .audience(audience)
                .claim("azp", authorizedParty)
                .build();
    }
}
