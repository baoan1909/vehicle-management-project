package com.ban.vehicle_management.infrastructure.security.keycloak.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.application.iam.account.model.command.CreateProvisionedAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class KeycloakIdentityProviderSecurityAdapterTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeFullNameWhenBuildingRegisterUserRequestBody() {
        Map<String, Object> requestBody = KeycloakIdentityProviderSecurityAdapter.buildRegisterUserRequestBody(
                new RegisterAccountCommand(
                        "customer01",
                        "customer01@example.com",
                        "Secret123!",
                        "Nguyen Van A"
                )
        );

        Map<String, List<String>> attributes = (Map<String, List<String>>) requestBody.get("attributes");

        assertEquals("Nguyen Van A", requestBody.get("firstName"));
        assertEquals(List.of("Nguyen Van A"), attributes.get("full_name"));
        assertEquals(List.of("vehicle-management"), attributes.get("source"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeFullNameWhenBuildingProvisionedUserRequestBody() {
        Account account = new Account();
        account.setUsername("employee01");
        account.setEmail("employee01@example.com");

        Map<String, Object> requestBody = KeycloakIdentityProviderSecurityAdapter.buildProvisionedUserRequestBody(
                new CreateProvisionedAccountCommand(
                        account,
                        null,
                        AdminProvisionableAccountRoleCode.EMPLOYEE,
                        "Tran Thi B"
                )
        );

        Map<String, List<String>> attributes = (Map<String, List<String>>) requestBody.get("attributes");

        assertEquals("Tran Thi B", requestBody.get("firstName"));
        assertEquals(List.of("Tran Thi B"), attributes.get("full_name"));
        assertEquals(List.of("PROVISIONED"), attributes.get("account_type"));
    }

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
