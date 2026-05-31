package com.ban.vehicle_management.infrastructure.security.keycloak.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KeycloakIdentityProviderSecurityAdapter implements IdentityProviderAdminPortOut {

    private final RestClient restClient;
    private final String baseUrl;
    private final String realm;
    private final String frontendClientId;
    private final String postActionRedirectUri;
    private final String adminClientId;
    private final String adminClientSecret;

    public KeycloakIdentityProviderSecurityAdapter(
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
            KeycloakUserRepresentation currentUser = getUserRepresentation(keycloakUserId);
            if (currentUser == null || currentUser.username() == null || currentUser.username().isBlank()) {
                throw new BadRequestException("Keycloak user is missing username");
            }

            Map<String, List<String>> mergedAttributes = new HashMap<>();
            if (currentUser.attributes() != null) {
                mergedAttributes.putAll(currentUser.attributes());
            }
            mergedAttributes.put("account_id", List.of(accountId.toString()));
            mergedAttributes.putIfAbsent("source", List.of("vehicle-management"));

            restClient.put()
                    .uri(baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new KeycloakUserUpdateRequest(
                            currentUser.username(),
                            currentUser.email(),
                            currentUser.enabled(),
                            currentUser.emailVerified(),
                            currentUser.firstName(),
                            currentUser.lastName(),
                            currentUser.requiredActions(),
                            mergedAttributes
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (BadRequestException exception) {
            throw exception;
        } catch (HttpClientErrorException exception) {
            throw new BadRequestException("Failed to sync account_id to Keycloak");
        }
    }

    @Override
    public void sendVerifyEmail(String keycloakUserId) {
        try {
            restClient.put()
                    .uri(buildSendVerifyEmailUri(keycloakUserId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            String errorBody = exception.getResponseBodyAsString();
            if (errorBody == null || errorBody.isBlank()) {
                throw new BadRequestException("Failed to trigger Keycloak verify email");
            }
            throw new BadRequestException("Failed to trigger Keycloak verify email: " + errorBody);
        }
    }

    @Override
    public void sendUpdatePasswordEmail(String keycloakUserId) {
        executeActionsEmail(keycloakUserId, List.of("UPDATE_PASSWORD"));
    }

    @Override
    public boolean isEmailVerified(String keycloakUserId) {
        try {
            KeycloakUserResponse user = restClient.get()
                    .uri(baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                    .retrieve()
                    .body(KeycloakUserResponse.class);
            return user != null && Boolean.TRUE.equals(user.emailVerified());
        } catch (HttpClientErrorException.NotFound exception) {
            return false;
        } catch (HttpClientErrorException exception) {
            throw new BadRequestException("Failed to read Keycloak user verification state");
        }
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
            String errorBody = exception.getResponseBodyAsString();
            if (errorBody == null || errorBody.isBlank()) {
                throw new BadRequestException("Failed to trigger Keycloak email action");
            }
            throw new BadRequestException("Failed to trigger Keycloak email action: " + errorBody);
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

    private URI buildSendVerifyEmailUri(String keycloakUserId) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/admin/realms/{realm}/users/{keycloakUserId}/send-verify-email")
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

    private KeycloakUserRepresentation getUserRepresentation(String keycloakUserId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/admin/realms/" + realm + "/users/" + keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAdminAccessToken())
                    .retrieve()
                    .body(KeycloakUserRepresentation.class);
        } catch (HttpClientErrorException exception) {
            throw new BadRequestException("Failed to read Keycloak user profile before update");
        }
    }

    private record KeycloakTokenResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("access_token")
            String accessToken
    ) {
    }

    private record KeycloakUserResponse(
            Boolean emailVerified
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KeycloakUserRepresentation(
            String username,
            String email,
            Boolean enabled,
            Boolean emailVerified,
            String firstName,
            String lastName,
            List<String> requiredActions,
            Map<String, List<String>> attributes
    ) {
    }

    private record KeycloakUserUpdateRequest(
            String username,
            String email,
            Boolean enabled,
            Boolean emailVerified,
            String firstName,
            String lastName,
            List<String> requiredActions,
            Map<String, List<String>> attributes
    ) {
    }
}
