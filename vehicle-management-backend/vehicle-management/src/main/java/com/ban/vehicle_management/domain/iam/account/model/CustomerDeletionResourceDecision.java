package com.ban.vehicle_management.domain.iam.account.model;

import java.util.List;

public record CustomerDeletionResourceDecision(
        List<AccountDeletionBlocker> blockers,
        List<AccountDeletionAutomaticAction> automaticActions
) {
    public CustomerDeletionResourceDecision {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        automaticActions = automaticActions == null ? List.of() : List.copyOf(automaticActions);
    }

    public static CustomerDeletionResourceDecision blocker(AccountDeletionBlocker blocker) {
        return new CustomerDeletionResourceDecision(List.of(blocker), List.of());
    }

    public static CustomerDeletionResourceDecision automatic(AccountDeletionAutomaticAction action) {
        return new CustomerDeletionResourceDecision(List.of(), List.of(action));
    }
}
