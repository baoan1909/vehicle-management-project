package com.ban.vehicle_management.application.iam.account.port.in;

import java.util.UUID;

/** Persists a replayable signal that tells affected accounts to reload current-access. */
public interface AccountAccessInvalidationPortIn {

    void invalidateAccount(
            UUID accountId,
            String reason,
            String relatedSchema,
            String relatedTable,
            UUID relatedId
    );

    void invalidateActiveAccountsForRole(String roleCode, UUID roleId, String reason);
}
