package com.ban.vehicle_management.domain.iam.account.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountDeletionTombstonesTest {

    @Test
    void createsNonNullUniqueSafeMarkersFromAccountId() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertEquals("_deleted_" + first, AccountDeletionTombstones.marker(first));
        assertEquals("_deleted_" + first + "@deleted.invalid", AccountDeletionTombstones.email(first));
        assertFalse(AccountDeletionTombstones.marker(first).equals(AccountDeletionTombstones.marker(second)));
    }
}
