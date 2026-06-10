package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.application.iam.account.model.command.CreateProvisionedAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.ProvisionedAccountFilterCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateProvisionedAccountRoleCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateProvisionedAccountStatusCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;

import java.util.List;
import java.util.UUID;

public interface ProvisionedAccountPortIn {

    ProvisionedAccountResult createProvisionedAccount(CreateProvisionedAccountCommand command);

    List<ProvisionedAccountResult> getProvisionedAccounts(ProvisionedAccountFilterCommand command);

    ProvisionedAccountResult getProvisionedAccountById(UUID accountId);

    ProvisionedAccountResult updateProvisionedAccountStatus(
            UUID accountId,
            UpdateProvisionedAccountStatusCommand command
    );

    ProvisionedAccountResult updateProvisionedAccountRole(UUID accountId, UpdateProvisionedAccountRoleCommand command);
}
