package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Loads internal account state and resolved permission data from persistence.
 * Keycloak remains the authentication provider, while authorization is resolved
 * from the local vehicle-management database.
 */
public interface AccountAuthorizationPort {

    Optional<CurrentAccountAccess> findByAccountId(UUID accountId);

    Optional<CurrentAccountAccess> findByKeycloakUserId(String keycloakUserId);

    Set<String> findPermissionCodesByAccountId(UUID accountId);
}
