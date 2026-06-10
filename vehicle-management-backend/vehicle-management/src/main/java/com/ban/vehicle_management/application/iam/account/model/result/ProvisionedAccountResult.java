package com.ban.vehicle_management.application.iam.account.model.result;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProvisionedAccountResult(
        AccountInfoResult account,
        RoleInfoResult role
) {
    public record AccountInfoResult(
            UUID accountId,
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
}
