package com.ban.vehicle_management.application.iam.account.model.command;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;

public record UpdateProvisionedAccountStatusCommand(
        AccountStatus status,
        String reason
) {
}
