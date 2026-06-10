package com.ban.vehicle_management.entrypoint.dto.iam.account.request;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;

public record UpdateProvisionedAccountStatusRequest(
        AccountStatus status,
        String reason
) {
}
