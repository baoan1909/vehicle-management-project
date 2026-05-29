package com.ban.vehicle_management.infrastructure.security;

import java.util.UUID;

/**
 * Security principal projection used by the resource server layer before the
 * application starts loading richer authorization data from the local database.
 */
public record AuthenticatedAccountPrincipal(
        UUID accountId,
        String keycloakUserId,
        String preferredUsername,
        String email
) {
}
