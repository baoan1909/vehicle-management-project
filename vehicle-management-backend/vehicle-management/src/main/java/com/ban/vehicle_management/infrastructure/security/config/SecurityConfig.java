package com.ban.vehicle_management.infrastructure.security.config;

import com.ban.vehicle_management.infrastructure.security.jwt.JwtAudienceValidator;
import com.ban.vehicle_management.infrastructure.security.jwt.JwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final String issuerUri;
    private final String jwkSetUri;
    private final String audience;
    private final String acceptedAuthorizedParties;
    private final int jwkConnectTimeoutMs;
    private final int jwkReadTimeoutMs;
    private final String corsAllowedOrigins;

    public SecurityConfig(
            JwtAuthenticationConverter jwtAuthenticationConverter,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${app.security.oauth2.audience:}") String audience,
            @Value("${app.security.oauth2.accepted-authorized-parties:vehicle-management-frontend}") String acceptedAuthorizedParties,
            @Value("${app.security.oauth2.jwk.connect-timeout-ms:3000}") int jwkConnectTimeoutMs,
            @Value("${app.security.oauth2.jwk.read-timeout-ms:15000}") int jwkReadTimeoutMs,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String corsAllowedOrigins
    ) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
        this.audience = audience;
        this.acceptedAuthorizedParties = acceptedAuthorizedParties;
        this.jwkConnectTimeoutMs = jwkConnectTimeoutMs;
        this.jwkReadTimeoutMs = jwkReadTimeoutMs;
        this.corsAllowedOrigins = corsAllowedOrigins;
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
                                "/api/dev/mail-preview/**",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/ws",
                                "/ws/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/public/pricing/**",
                                "/api/public/payments/vnpay/**",
                                "/api/notifications/broadcast-announcements/active"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );
        return http.build();
    }

    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsFilter(corsConfigurationSource);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseCsv(corsAllowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Idempotency-Key"
        ));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String resolvedJwkSetUri = resolveJwkSetUri();
        if (!StringUtils.hasText(resolvedJwkSetUri)) {
            throw new IllegalStateException("Either issuer-uri or jwk-set-uri must be configured for JWT decoding");
        }

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(resolvedJwkSetUri)
                .restOperations(jwtRestOperations())
                .cache(new ConcurrentMapCache("keycloak-jwk-set-cache"))
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> validator;
        if (StringUtils.hasText(issuerUri)) {
            validator = JwtValidators.createDefaultWithIssuer(issuerUri);
        } else {
            validator = JwtValidators.createDefault();
        }

        List<String> acceptedAudiences = parseCsv(audience);
        List<String> acceptedParties = parseCsv(acceptedAuthorizedParties);
        if (!acceptedAudiences.isEmpty() || !acceptedParties.isEmpty()) {
            validator = new DelegatingOAuth2TokenValidator<>(
                    validator,
                    new JwtAudienceValidator(acceptedAudiences, acceptedParties)
            );
        }

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    private RestOperations jwtRestOperations() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(jwkConnectTimeoutMs);
        requestFactory.setReadTimeout(jwkReadTimeoutMs);
        return new RestTemplate(requestFactory);
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

    private List<String> parseCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
