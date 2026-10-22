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
public interface AccountAuthorizationPortOut {

    Optional<CurrentAccountAccess> findByAccountId(UUID accountId);

    Optional<CurrentAccountAccess> findByKeycloakUserId(String keycloakUserId);

    /**
     * Returns the account that provisioned the target account. This ownership
     * information is used by scoped approval flows; it is not a client-visible
     * field.
     */
    Optional<UUID> findCreatedByAccountId(UUID accountId);

    Set<String> findPermissionCodesByAccountId(UUID accountId);
}
