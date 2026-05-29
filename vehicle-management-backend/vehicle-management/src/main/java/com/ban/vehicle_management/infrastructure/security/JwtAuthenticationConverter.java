package com.ban.vehicle_management.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Optional
                .ofNullable(jwtGrantedAuthoritiesConverter.convert(jwt))
                .orElseGet(List::of);
        AuthenticatedAccountPrincipal principal = new AuthenticatedAccountPrincipal(
                parseUuid(jwt.getClaim("account_id")).orElse(null),
                jwt.getSubject(),
                resolvePreferredUsername(jwt),
                jwt.getClaimAsString("email")
        );

        JwtAuthenticationToken authenticationToken = new JwtAuthenticationToken(
                jwt,
                authorities,
                principal.preferredUsername()
        );

        authenticationToken.setDetails(principal);
        return authenticationToken;
    }

    private String resolvePreferredUsername(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }

        String username = jwt.getClaimAsString("username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return jwt.getSubject();
    }

    private Optional<UUID> parseUuid(Object claimValue) {
        if (claimValue instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (claimValue instanceof String text && !text.isBlank()) {
            try{
                return Optional.of(UUID.fromString(text));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
