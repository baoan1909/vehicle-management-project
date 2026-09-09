package com.ban.vehicle_management.infrastructure.security.adapter;

import com.ban.vehicle_management.application.iam.account.model.security.AuthenticatedSocialIdentity;
import com.ban.vehicle_management.application.iam.account.port.in.AuthenticatedSocialIdentityPortIn;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedSocialIdentitySecurityAdapter implements AuthenticatedSocialIdentityPortIn {

    @Override
    public AuthenticatedSocialIdentity getAuthenticatedIdentityOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
                || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated JWT could not be resolved");
        }

        Jwt jwt = jwtAuthenticationToken.getToken();
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated JWT is missing subject");
        }

        return new AuthenticatedSocialIdentity(
                subject,
                jwt.getClaimAsString("identity_provider"),
                jwt.getClaimAsString("email"),
                booleanClaim(jwt.getClaim("email_verified")),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name")
        );
    }

    private boolean booleanClaim(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value instanceof String stringValue && Boolean.parseBoolean(stringValue);
    }
}
