package com.ban.vehicle_management.application.iam.account.model.command;

import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;

public record UpdateProvisionedAccountRoleCommand(
        AdminProvisionableAccountRoleCode roleCode
) {
}
