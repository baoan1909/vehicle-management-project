package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.CreateProvisionedAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.ProvisionedAccountFilterCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateProvisionedAccountRoleCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateProvisionedAccountStatusCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.ProvisionedAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.ProvisionedAccountPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisionedAccountUseCaseImpl implements ProvisionedAccountPortIn {

    private static final String ACCOUNT_CREATE_ALL = "ACCOUNT_CREATE_ALL";
    private static final String ACCOUNT_READ_ALL = "ACCOUNT_READ_ALL";
    private static final String ACCOUNT_UPDATE_ALL = "ACCOUNT_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ProvisionedAccountPortOut provisionedAccountPortOut;
    private final IdentityProviderAdminPortOut identityProviderAdminPortOut;

    public ProvisionedAccountUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ProvisionedAccountPortOut provisionedAccountPortOut,
            IdentityProviderAdminPortOut identityProviderAdminPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.provisionedAccountPortOut = provisionedAccountPortOut;
        this.identityProviderAdminPortOut = identityProviderAdminPortOut;
    }

    @Override
    @Transactional
    public ProvisionedAccountResult createProvisionedAccount(CreateProvisionedAccountCommand command) {
        currentAccountPortIn.requirePermission(ACCOUNT_CREATE_ALL);

        CreateProvisionedAccountCommand normalizedCommand = normalizeCreateCommand(command);
        ensureCreateNoConflict(normalizedCommand);

        UUID accountId = UUID.randomUUID();
        UUID roleId = provisionedAccountPortOut.findActiveRoleIdByCode(normalizedCommand.roleCode());

        String keycloakUserId = identityProviderAdminPortOut.createProvisionedAccountUser(normalizedCommand);
        try {
            Account account = initializeAccount(
                    normalizedCommand.account(),
                    accountId,
                    roleId,
                    keycloakUserId
            );
            provisionedAccountPortOut.provisionAccount(account);
            identityProviderAdminPortOut.updateAccountIdAttribute(keycloakUserId, accountId);
            identityProviderAdminPortOut.sendUpdatePasswordEmail(keycloakUserId);
            return provisionedAccountPortOut.findProvisionedAccountById(accountId)
                    .orElseThrow(() -> new NotFoundException("Provisioned account not found"));
        } catch (RuntimeException exception) {
            identityProviderAdminPortOut.deleteUser(keycloakUserId);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvisionedAccountResult> getProvisionedAccounts(ProvisionedAccountFilterCommand command) {
        currentAccountPortIn.requirePermission(ACCOUNT_READ_ALL);
        return provisionedAccountPortOut.findProvisionedAccounts(normalizeFilterCommand(command));
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisionedAccountResult getProvisionedAccountById(UUID accountId) {
        currentAccountPortIn.requirePermission(ACCOUNT_READ_ALL);
        return getManagedAccount(accountId);
    }

    @Override
    @Transactional
    public ProvisionedAccountResult updateProvisionedAccountStatus(
            UUID accountId,
            UpdateProvisionedAccountStatusCommand command
    ) {
        currentAccountPortIn.requirePermission(ACCOUNT_UPDATE_ALL);

        ProvisionedAccountResult existingAccount = getManagedAccount(accountId);
        UpdateProvisionedAccountStatusCommand normalizedCommand = normalizeStatusCommand(command);
        validateStatusTransition(existingAccount.account().accountStatus(), normalizedCommand.status());
        AccountStatus targetStatus = normalizedCommand.status();

        provisionedAccountPortOut.updateProvisionedAccountStatus(
                accountId,
                targetStatus,
                toUserProfileStatus(targetStatus),
                toCustomerStatus(targetStatus),
                toEmployeeStatus(existingAccount.role().roleCode(), targetStatus),
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                normalizedCommand.reason()
        );
        identityProviderAdminPortOut.updateUserEnabled(
                existingAccount.account().keycloakUserId(),
                AccountStatus.ACTIVE.equals(targetStatus)
        );

        return provisionedAccountPortOut.findProvisionedAccountById(accountId)
                .orElseThrow(() -> new NotFoundException("Provisioned account not found"));
    }

    @Override
    @Transactional
    public ProvisionedAccountResult updateProvisionedAccountRole(
            UUID accountId,
            UpdateProvisionedAccountRoleCommand command
    ) {
        currentAccountPortIn.requirePermission(ACCOUNT_UPDATE_ALL);

        ProvisionedAccountResult existingAccount = getManagedAccount(accountId);
        UpdateProvisionedAccountRoleCommand normalizedCommand = normalizeRoleCommand(command);
        validateRoleTransition(existingAccount.role().roleCode(), normalizedCommand.roleCode());
        UUID roleId = provisionedAccountPortOut.findActiveRoleIdByCode(normalizedCommand.roleCode());
        provisionedAccountPortOut.updateProvisionedAccountRole(accountId, roleId);

        return provisionedAccountPortOut.findProvisionedAccountById(accountId)
                .orElseThrow(() -> new NotFoundException("Provisioned account not found"));
    }

    private void ensureCreateNoConflict(CreateProvisionedAccountCommand command) {
        if (provisionedAccountPortOut.existsByUsername(command.account().getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (provisionedAccountPortOut.existsByEmail(command.account().getEmail())) {
            throw new ConflictException("Email already exists");
        }
    }

    private CreateProvisionedAccountCommand normalizeCreateCommand(CreateProvisionedAccountCommand command) {
        requireField(command, "command");
        return new CreateProvisionedAccountCommand(
                normalizeAccount(command.account()),
                null,
                requireProvisionableRole(command.roleCode())
        );
    }

    private ProvisionedAccountFilterCommand normalizeFilterCommand(ProvisionedAccountFilterCommand command) {
        requireField(command, "command");
        return new ProvisionedAccountFilterCommand(
                TextValidationUtils.normalizeNullableText(command.keyword(), "keyword", 100),
                command.roleCode(),
                command.accountStatus()
        );
    }

    private UpdateProvisionedAccountStatusCommand normalizeStatusCommand(UpdateProvisionedAccountStatusCommand command) {
        requireField(command, "command");
        AccountStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new BadRequestException("status must not be null");
        }
        if (AccountStatus.PENDING.equals(targetStatus)) {
            throw new BadRequestException("Provisioned accounts do not support PENDING status");
        }
        return new UpdateProvisionedAccountStatusCommand(
                targetStatus,
                TextValidationUtils.normalizeNullableText(command.reason(), "reason", 255)
        );
    }

    private UpdateProvisionedAccountRoleCommand normalizeRoleCommand(UpdateProvisionedAccountRoleCommand command) {
        requireField(command, "command");
        return new UpdateProvisionedAccountRoleCommand(requireProvisionableRole(command.roleCode()));
    }

    private Account normalizeAccount(Account account) {
        requireField(account, "account");
        Account normalizedAccount = new Account();
        normalizedAccount.setUsername(
                TextValidationUtils.normalizeRequiredText(account.getUsername(), "username", 100)
        );
        normalizedAccount.setEmail(normalizeRequiredEmail(account.getEmail()));
        return normalizedAccount;
    }

    private Account initializeAccount(
            Account account,
            UUID accountId,
            UUID roleId,
            String keycloakUserId
    ) {
        account.setAccountId(accountId);
        account.setUserProfileId(null);
        account.setKeycloakUserId(keycloakUserId);
        account.setRoleId(roleId);
        account.setStatus(AccountStatus.ACTIVE);
        account.setFailedLoginCount(0);
        return account;
    }

    private ProvisionedAccountResult getManagedAccount(UUID accountId) {
        ProvisionedAccountResult result = provisionedAccountPortOut.findProvisionedAccountById(accountId)
                .orElseThrow(() -> new NotFoundException("Provisioned account not found"));
        ensureManagedAccount(result);
        return result;
    }

    private void ensureManagedAccount(ProvisionedAccountResult result) {
        if (result == null || result.role() == null || result.role().roleCode() == null) {
            throw new NotFoundException("Provisioned account not found");
        }
        try {
            AdminProvisionableAccountRoleCode.valueOf(result.role().roleCode());
        } catch (IllegalArgumentException exception) {
            throw new NotFoundException("Provisioned account not found");
        }
    }

    private UserProfileStatus toUserProfileStatus(AccountStatus accountStatus) {
        return switch (accountStatus) {
            case ACTIVE -> UserProfileStatus.ACTIVE;
            case LOCKED -> UserProfileStatus.SUSPENDED;
            case DISABLED -> UserProfileStatus.INACTIVE;
            case PENDING -> throw new BadRequestException("Provisioned accounts do not support PENDING status");
        };
    }

    private EmployeeStatus toEmployeeStatus(String roleCode, AccountStatus accountStatus) {
        AdminProvisionableAccountRoleCode currentRole = parseProvisionableRole(roleCode);
        if (currentRole.requiresEmployeeRecord()) {
            return null;
        }
        return switch (accountStatus) {
            case ACTIVE -> EmployeeStatus.ACTIVE;
            case LOCKED -> EmployeeStatus.SUSPENDED;
            case DISABLED -> EmployeeStatus.INACTIVE;
            case PENDING -> throw new BadRequestException("Provisioned accounts do not support PENDING status");
        };
    }

    private CustomerStatus toCustomerStatus(AccountStatus accountStatus) {
        return switch (accountStatus) {
            case ACTIVE -> CustomerStatus.ACTIVE;
            case LOCKED, DISABLED -> CustomerStatus.INACTIVE;
            case PENDING -> throw new BadRequestException("Provisioned accounts do not support PENDING status");
        };
    }

    private void validateStatusTransition(AccountStatus currentStatus, AccountStatus targetStatus) {
        if (AccountStatus.DISABLED.equals(currentStatus) && AccountStatus.LOCKED.equals(targetStatus)) {
            throw new BadRequestException("Provisioned accounts cannot transition from DISABLED to LOCKED");
        }
    }

    private void validateRoleTransition(String currentRoleCode, AdminProvisionableAccountRoleCode targetRoleCode) {
        AdminProvisionableAccountRoleCode currentRole = parseProvisionableRole(currentRoleCode);
        if (currentRole.isInternalRole() != targetRoleCode.isInternalRole()) {
            throw new BadRequestException(
                    "Changing role between CUSTOMER and internal account types is not supported"
            );
        }
    }

    private AdminProvisionableAccountRoleCode requireProvisionableRole(AdminProvisionableAccountRoleCode roleCode) {
        if (roleCode == null) {
            throw new BadRequestException("roleCode must not be null");
        }
        return roleCode;
    }

    private AdminProvisionableAccountRoleCode parseProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new BadRequestException("Current account role is not supported");
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Current account role is not supported");
        }
    }

    private String normalizeRequiredEmail(String email) {
        return TextValidationUtils.normalizeRequiredText(email, "email", 255)
                .toLowerCase(Locale.ROOT);
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
