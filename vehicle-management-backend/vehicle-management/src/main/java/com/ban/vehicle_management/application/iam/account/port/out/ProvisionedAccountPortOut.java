package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.application.iam.account.model.command.ProvisionedAccountFilterCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvisionedAccountPortOut {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    UUID findActiveRoleIdByCode(AdminProvisionableAccountRoleCode roleCode);

    void provisionAccount(Account account);

    List<ProvisionedAccountResult> findProvisionedAccounts(ProvisionedAccountFilterCommand command);

    Optional<ProvisionedAccountResult> findProvisionedAccountById(UUID accountId);

    void updateProvisionedAccountStatus(
            UUID accountId,
            AccountStatus accountStatus,
            UserProfileStatus userProfileStatus,
            CustomerStatus customerStatus,
            EmployeeStatus employeeStatus,
            UUID changedBy,
            String reason
    );

    void updateProvisionedAccountRole(UUID accountId, UUID roleId);
}
