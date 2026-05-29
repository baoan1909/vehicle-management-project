package com.ban.vehicle_management.infrastructure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.List;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
            "invalid_token",
            "The required audience is missing",
            null
    );

    private final String requiredAudience;

    public JwtAudienceValidator(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if(!StringUtils.hasText(requiredAudience)) {
            return OAuth2TokenValidatorResult.success();
        }

        List<String> audiences = token.getAudience();
        if(audiences != null && audiences.contains(requiredAudience)) {
            return OAuth2TokenValidatorResult.success();
        }

        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }
}
