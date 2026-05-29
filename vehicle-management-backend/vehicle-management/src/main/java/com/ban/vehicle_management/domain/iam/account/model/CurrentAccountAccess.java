package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;

import java.util.Set;
import java.util.UUID;

public record CurrentAccountAccess(
        UUID accountId,
        String subject,
        String username,
        String email,
        UUID roleId,
        AccountStatus status,
        Set<String> permissionCodes
) {
}
