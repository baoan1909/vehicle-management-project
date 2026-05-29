package com.ban.vehicle_management.infrastructure.security;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPort;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KeycloakIdentityAdminAdapter implements IdentityProviderAdminPort {

    private final RestClient restClient;
    private final String baseUrl;
    private final String realm;
    private final String frontendClientId;
    private final String postActionRedirectUri;
    private final String adminClientId;
    private final String adminClientSecret;

    public KeycloakIdentityAdminAdapter(
            @Value("${app.keycloak.base-url}") String baseUrl,
            @Value("${app.keycloak.realm}") String realm,
            @Value("${app.keycloak.frontend-client-id}") String frontendClientId,
            @Value("${app.keycloak.post-action-redirect-uri:}") String postActionRedirectUri,
            @Value("${app.keycloak.admin-client-id}") String adminClientId,
            @Value("${app.keycloak.admin-client-secret}") String adminClientSecret
    ) {
        this.restClient = RestClient.builder().build();
        this.baseUrl = baseUrl;
        this.realm = realm;
        this.frontendClientId = frontendClientId;
        this.postActionRedirectUri = postActionRedirectUri;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
    }

    @Override
    public String createUser(RegisterAccountCommand command) {
        try {
            URI location = restClient.post()
                    .uri(baseUrl + "/admin/realms/" + realm + "/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "username", command.username(),
                            "email", command.email(),
                            "enabled", true,
                            "emailVerified", false,
                            "requiredActions", List.of("VERIFY_EMAIL"),
                            "attributes", Map.of(
                                    "source", List.of("vehicle-management")
                            ),
                            "credentials", List.of(Map.of(
                                    "type", "password",
                                    "value", command.password(),
                                    "temporary", false
                            ))
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();

            if (location == null) {
                throw new BadRequestException("Keycloak did not return a user location");
            }

            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (HttpClientErrorException.Conflict exception) {
            throw new ConflictException("Keycloak user already exists");
        } catch (HttpClientErrorException exception) {
            throw new BadRequestException("Failed to create Keycloak user");
        }
    }

    @Override
    public void updateAccountIdAttribute(String keycloakUserId, UUID accountId) {
        try {
            restClient.put()
                    .uri(baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "attributes", Map.of(
                                    "account_id", List.of(accountId.toString()),
                                    "source", List.of("vehicle-management")
                            )
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            throw new BadRequestException("Failed to sync account_id to Keycloak");
        }
    }

    @Override
    public void sendVerifyEmail(String keycloakUserId) {
        executeActionsEmail(keycloakUserId, List.of("VERIFY_EMAIL"));
    }

    @Override
    public void sendUpdatePasswordEmail(String keycloakUserId) {
        executeActionsEmail(keycloakUserId, List.of("UPDATE_PASSWORD"));
    }

    @Override
    public void deleteUser(String keycloakUserId) {
        try {
            restClient.delete()
                    .uri(baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ignored) {
        }
    }

    private void executeActionsEmail(String keycloakUserId, List<String> actions) {
        try {
            restClient.put()
                    .uri(buildExecuteActionsEmailUri(keycloakUserId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(actions)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            throw new BadRequestException("Failed to trigger Keycloak email action");
        }
    }

    private URI buildExecuteActionsEmailUri(String keycloakUserId) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/admin/realms/{realm}/users/{keycloakUserId}/execute-actions-email")
                .queryParam("client_id", frontendClientId);

        if (StringUtils.hasText(postActionRedirectUri)) {
            uriBuilder.queryParam("redirect_uri", postActionRedirectUri);
        }

        return uriBuilder.buildAndExpand(realm, keycloakUserId).toUri();
    }

    private String getAdminAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);

        KeycloakTokenResponse tokenResponse = restClient.post()
                .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KeycloakTokenResponse.class);

        if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            throw new BadRequestException("Failed to obtain Keycloak admin token");
        }

        return tokenResponse.accessToken();
    }

    private record KeycloakTokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token")
            String accessToken
    ) {
    }
}
