package com.ban.vehicle_management.entrypoint.dto.iam.account.response;

import java.util.Set;
import java.util.UUID;

public record CurrentAccountAccessResponse(
        UUID accountId,
        String username,
        String email,
        UUID roleId,
        String roleCode,
        String accountStatus,
        String employeeStatus,
        String customerStatus,
        String customerApprovalStatus,
        Set<String> permissionCodes
) {
}
