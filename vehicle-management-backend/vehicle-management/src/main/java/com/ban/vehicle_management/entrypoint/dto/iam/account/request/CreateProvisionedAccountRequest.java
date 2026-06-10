package com.ban.vehicle_management.entrypoint.dto.iam.account.request;

import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
public record CreateProvisionedAccountRequest(
        String username,
        String email,
        AdminProvisionableAccountRoleCode roleCode
) {
}
