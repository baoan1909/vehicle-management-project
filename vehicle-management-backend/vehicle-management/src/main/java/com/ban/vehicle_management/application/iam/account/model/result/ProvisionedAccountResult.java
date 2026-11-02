package com.ban.vehicle_management.application.iam.account.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProvisionedAccountResult(
        AccountInfoResult account,
        RoleInfoResult role,
        LifecycleInfoResult lifecycle
) {
    public ProvisionedAccountResult(AccountInfoResult account, RoleInfoResult role) {
        this(account, role, null);
    }

    public record AccountInfoResult(
            UUID accountId,
            UUID userProfileId,
            String keycloakUserId,
            String username,
            String email,
            AccountStatus accountStatus,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record RoleInfoResult(
            UUID roleId,
            String roleCode,
            String roleName,
            List<String> permissionCodes
    ) {
    }

    public record LifecycleInfoResult(
            String profileState,
            String approvalState,
            String workforceState,
            String scopeState,
            String operationalAccessState,
            String requiredAction,
            int effectivePermissionCount
    ) {
    }
}
