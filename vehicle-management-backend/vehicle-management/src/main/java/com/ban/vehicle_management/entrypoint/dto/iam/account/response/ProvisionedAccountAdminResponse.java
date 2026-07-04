package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.List;
import java.util.UUID;

public record ProvisionedAccountAdminResponse(
        AccountInfoResponse account,
        RoleInfoResponse role
) {
    public record AccountInfoResponse(
            UUID accountId,
            String keycloakUserId,
            String username,
            String email,
            String accountStatus,
            String createdAt,
            String updatedAt
    ) {
    }

    public record RoleInfoResponse(
            UUID roleId,
            String roleCode,
            String roleName,
            List<String> permissionCodes
    ) {
    }
}
