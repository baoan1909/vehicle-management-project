package com.ban.vehicle_management.infrastructure.security.jwt;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    private final AccountAuthorizationPortOut accountAuthorizationPortOut;

    public JwtAuthenticationConverter(AccountAuthorizationPortOut accountAuthorizationPortOut) {
        this.accountAuthorizationPortOut = accountAuthorizationPortOut;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>(Optional
                .ofNullable(jwtGrantedAuthoritiesConverter.convert(jwt))
                .orElseGet(List::of));
        resolveCurrentAccount(jwt)
                .filter(account -> AccountStatus.ACTIVE.equals(account.status()))
                .ifPresent(account -> account.permissionCodes().forEach(permissionCode ->
                        authorities.add(new SimpleGrantedAuthority(permissionCode))
                ));
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

    private Optional<CurrentAccountAccess> resolveCurrentAccount(Jwt jwt) {
        Optional<UUID> accountId = parseUuid(jwt.getClaim("account_id"));
        if (accountId.isPresent()) {
            return accountAuthorizationPortOut.findByAccountId(accountId.get());
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        return accountAuthorizationPortOut.findByKeycloakUserId(subject);
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
