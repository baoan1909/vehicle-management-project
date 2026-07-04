package com.ban.vehicle_management.application.iam.account.model.command;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;

public record CreateProvisionedAccountCommand(
        Account account,
        String password,
        AdminProvisionableAccountRoleCode roleCode,
        String fullName
) {
}
