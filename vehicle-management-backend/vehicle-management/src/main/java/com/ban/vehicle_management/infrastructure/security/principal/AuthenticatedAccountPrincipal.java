package com.ban.vehicle_management.infrastructure.security.principal;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import java.util.UUID;

/**
 * Security principal projection used by the resource server layer before the
 * application starts loading richer authorization data from the local database.
 */
public record AuthenticatedAccountPrincipal(
        UUID accountId,
        String keycloakUserId,
        String preferredUsername,
        String email,
        CurrentAccountAccess accessSnapshot
) {

    public AuthenticatedAccountPrincipal(
            UUID accountId,
            String keycloakUserId,
            String preferredUsername,
            String email
    ) {
        this(accountId, keycloakUserId, preferredUsername, email, null);
    }

    public boolean hasConsistentSnapshot() {
        return accessSnapshot == null
                || (accountId != null && accountId.equals(accessSnapshot.accountId()));
    }
}
