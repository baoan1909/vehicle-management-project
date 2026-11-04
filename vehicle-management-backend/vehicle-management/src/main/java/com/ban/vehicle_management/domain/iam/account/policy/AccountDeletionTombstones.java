package com.ban.vehicle_management.domain.iam.account.policy;

import java.util.UUID;

public final class AccountDeletionTombstones {

    private AccountDeletionTombstones() {
    }

    public static String marker(UUID accountId) {
        if (accountId == null) throw new IllegalArgumentException("accountId is required");
        return "_deleted_" + accountId;
    }

    public static String email(UUID accountId) {
        return marker(accountId) + "@deleted.invalid";
    }
}
