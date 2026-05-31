package com.ban.vehicle_management.infrastructure.security.config;

import com.ban.vehicle_management.infrastructure.security.jwt.JwtAudienceValidator;
import com.ban.vehicle_management.infrastructure.security.jwt.JwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final String issuerUri;
    private final String jwkSetUri;
    private final String audience;

    public SecurityConfig(
            JwtAuthenticationConverter jwtAuthenticationConverter,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${app.security.oauth2.audience:}") String audience
    ) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
        this.audience = audience;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/error",
                                "/api/public/auth/**",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String resolvedJwkSetUri = resolveJwkSetUri();
        if (!StringUtils.hasText(resolvedJwkSetUri)) {
            throw new IllegalStateException("Either issuer-uri or jwk-set-uri must be configured for JWT decoding");
        }

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(resolvedJwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> validator;
        if (StringUtils.hasText(issuerUri)) {
            validator = JwtValidators.createDefaultWithIssuer(issuerUri);
        } else {
            validator = JwtValidators.createDefault();
        }

        if (StringUtils.hasText(audience)) {
            validator = new DelegatingOAuth2TokenValidator<>(
                    validator,
                    new JwtAudienceValidator(audience)
            );
        }

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    private String resolveJwkSetUri() {
        if (StringUtils.hasText(jwkSetUri)) {
            return jwkSetUri;
        }

        if (!StringUtils.hasText(issuerUri)) {
            return null;
        }

        return issuerUri.replaceAll("/+$", "") + "/protocol/openid-connect/certs";
    }
}
