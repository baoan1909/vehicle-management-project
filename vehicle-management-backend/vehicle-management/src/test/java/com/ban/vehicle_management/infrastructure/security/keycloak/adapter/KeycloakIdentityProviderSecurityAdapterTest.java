package com.ban.vehicle_management.infrastructure.security.keycloak.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class KeycloakIdentityProviderSecurityAdapterTest {

    @Test
    void shouldIncludeKeycloakResponseBodyWhenAvailable() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"errorMessage\":\"Password policy not met\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        String message = KeycloakIdentityProviderSecurityAdapter.buildKeycloakClientErrorMessage(
                "Failed to create Keycloak user",
                exception
        );

        assertEquals(
                "Failed to create Keycloak user: {\"errorMessage\":\"Password policy not met\"}",
                message
        );
    }

    @Test
    void shouldFallbackToStatusCodeWhenKeycloakResponseBodyIsBlank() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        String message = KeycloakIdentityProviderSecurityAdapter.buildKeycloakClientErrorMessage(
                "Failed to create Keycloak user",
                exception
        );

        assertEquals("Failed to create Keycloak user (Keycloak status: 400)", message);
    }

    @Test
    void shouldDescribeLikelyMissingServiceAccountRolesForForbiddenAdminAction() {
        KeycloakIdentityProviderSecurityAdapter adapter = new KeycloakIdentityProviderSecurityAdapter(
                "http://localhost:8081",
                "vehicle-management",
                "vehicle-management-frontend",
                "http://localhost:5173/login",
                "vehicle-management-admin-service",
                "secret"
        );

        String message = adapter.buildKeycloakAdminForbiddenMessage("create users");

        assertEquals(
                "Keycloak admin client 'vehicle-management-admin-service' does not have permission to create users. "
                        + "Check Service account roles in realm-management, especially manage-users, view-users, and query-users.",
                message
        );
    }
}
