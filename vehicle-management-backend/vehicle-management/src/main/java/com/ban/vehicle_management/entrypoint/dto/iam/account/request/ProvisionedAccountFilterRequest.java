package com.ban.vehicle_management.entrypoint.dto.iam.account.request;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;

public record ProvisionedAccountFilterRequest(
        String keyword,
        AdminProvisionableAccountRoleCode roleCode,
        AccountStatus accountStatus
) {
}
