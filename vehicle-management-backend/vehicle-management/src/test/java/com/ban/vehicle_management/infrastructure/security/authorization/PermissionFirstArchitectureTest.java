package com.ban.vehicle_management.infrastructure.security.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PermissionFirstArchitectureTest {

    private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "com", "ban", "vehicle_management");

    @Test
    void securityAdaptersMustNotAuthorizeByKnownActorRole() throws IOException {
        List<Path> securitySources = List.of(
                MAIN_SOURCE.resolve("infrastructure/security/adapter/CurrentAccountSecurityAdapter.java"),
                MAIN_SOURCE.resolve("infrastructure/security/authorization/PermissionAuthorizer.java"),
                MAIN_SOURCE.resolve("infrastructure/security/jwt/JwtAuthenticationConverter.java")
        );

        for (Path sourcePath : securitySources) {
            String source = Files.readString(sourcePath);
            assertFalse(source.contains("hasRole("), sourcePath + " must not use role authorization");
            assertFalse(source.contains("SYSTEM_ADMIN"), sourcePath + " must not bypass permissions for SYSTEM_ADMIN");
            assertFalse(source.contains("PARTNER_ADMIN"), sourcePath + " must not bypass permissions for PARTNER_ADMIN");
        }
    }

    @Test
    void provisionedAccountActorScopeMustComeFromEffectivePermissions() throws IOException {
        String useCase = Files.readString(MAIN_SOURCE.resolve(
                "application/iam/account/usecase/ProvisionedAccountUseCaseImpl.java"));
        String domainPolicy = Files.readString(MAIN_SOURCE.resolve(
                "domain/iam/account/policy/ProvisionedAccountPolicy.java"));

        assertTrue(useCase.contains("managedTargetRolesForCurrentAccount"));
        assertTrue(useCase.contains("getEffectivePermissionCodes()"));
        assertTrue(useCase.contains("PARKING_SCOPE_PLATFORM"));
        assertTrue(useCase.contains("PARKING_SCOPE_PARTNER"));
        assertTrue(useCase.contains("PARKING_SCOPE_ASSIGNED"));
        assertFalse(useCase.contains("getCurrentAccountOrThrow().roleCode()"));
        assertFalse(domainPolicy.contains("managedTargetRoles("),
                "A role-to-scope actor policy must not be reintroduced");
        assertFalse(domainPolicy.contains("canManageTargetRole("),
                "Current actor target management must stay permission/scope based");
    }
}
