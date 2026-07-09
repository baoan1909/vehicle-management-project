package com.ban.vehicle_management.infrastructure.security.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_TOKEN_CLIENT = new OAuth2Error(
            "invalid_token",
            "The token audience or authorized party is not accepted",
            null
    );

    private final Set<String> acceptedAudiences;
    private final Set<String> acceptedAuthorizedParties;

    public JwtAudienceValidator(
            Collection<String> acceptedAudiences,
            Collection<String> acceptedAuthorizedParties
    ) {
        this.acceptedAudiences = normalize(acceptedAudiences);
        this.acceptedAuthorizedParties = normalize(acceptedAuthorizedParties);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if(acceptedAudiences.isEmpty() && acceptedAuthorizedParties.isEmpty()) {
            return OAuth2TokenValidatorResult.success();
        }

        List<String> audiences = token.getAudience();
        if(audiences != null && audiences.stream().anyMatch(acceptedAudiences::contains)) {
            return OAuth2TokenValidatorResult.success();
        }

        String authorizedParty = firstNonBlank(
                token.getClaimAsString("azp"),
                token.getClaimAsString("client_id")
        );
        if(StringUtils.hasText(authorizedParty) && acceptedAuthorizedParties.contains(authorizedParty.trim())) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(INVALID_TOKEN_CLIENT);
    }

    private Set<String> normalize(Collection<String> values) {
        if(values == null) {
            return Set.of();
        }

        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }
}
