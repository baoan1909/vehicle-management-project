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
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.policy.ProvisionedAccountPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
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
    private final ProvisionedAccountPolicy provisionedAccountPolicy;
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();

    public ProvisionedAccountUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ProvisionedAccountPortOut provisionedAccountPortOut,
            IdentityProviderAdminPortOut identityProviderAdminPortOut,
            ProvisionedAccountPolicy provisionedAccountPolicy
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.provisionedAccountPortOut = provisionedAccountPortOut;
        this.identityProviderAdminPortOut = identityProviderAdminPortOut;
        this.provisionedAccountPolicy = provisionedAccountPolicy;
    }

    @Override
    @Transactional
    public ProvisionedAccountResult createProvisionedAccount(CreateProvisionedAccountCommand command) {
        currentAccountPortIn.requirePermission(ACCOUNT_CREATE_ALL);

        CreateProvisionedAccountCommand normalizedCommand = normalizeCreateCommand(command);
        ensureCanManageTargetRole(normalizedCommand.roleCode());
        ensureCreateNoConflict(normalizedCommand);

        UUID accountId = UUID.randomUUID();
        UUID roleId = provisionedAccountPortOut.findActiveRoleIdByCode(normalizedCommand.roleCode());

        String keycloakUserId = identityProviderAdminPortOut.createProvisionedAccountUser(normalizedCommand);
        try {
            Account account = initializeAccount(
                    normalizedCommand.account(),
                    accountId,
                    roleId,
                    normalizedCommand.roleCode(),
                    keycloakUserId
            );
            provisionedAccountPortOut.provisionAccount(
                    account,
                    buildMinimalUserProfile(account.getUserProfileId(), normalizedCommand.fullName())
            );
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
        return provisionedAccountPortOut.findProvisionedAccounts(normalizeFilterCommand(
                command,
                managedTargetRolesForCurrentAccount()
        ));
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
        provisionedAccountPolicy.validateStatusTransition(
                existingAccount.account().accountStatus(),
                normalizedCommand.status()
        );
        AccountStatus targetStatus = normalizedCommand.status();
        AdminProvisionableAccountRoleCode existingRole =
                provisionedAccountPolicy.requireProvisionableRole(existingAccount.role().roleCode());

        provisionedAccountPortOut.updateProvisionedAccountStatus(
                accountId,
                targetStatus,
                provisionedAccountPolicy.toUserProfileStatus(targetStatus),
                provisionedAccountPolicy.toCustomerStatus(targetStatus),
                provisionedAccountPolicy.toEmployeeStatus(existingRole, targetStatus),
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
        ensureCanManageTargetRole(normalizedCommand.roleCode());
        provisionedAccountPolicy.validateRoleTransition(
                provisionedAccountPolicy.requireProvisionableRole(existingAccount.role().roleCode()),
                normalizedCommand.roleCode()
        );
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
                requireProvisionableRole(command.roleCode()),
                TextValidationUtils.normalizeRequiredText(command.fullName(), "fullName", 150)
        );
    }

    private ProvisionedAccountFilterCommand normalizeFilterCommand(
            ProvisionedAccountFilterCommand command,
            Set<AdminProvisionableAccountRoleCode> managedRoleCodes
    ) {
        requireField(command, "command");
        return new ProvisionedAccountFilterCommand(
                TextValidationUtils.normalizeNullableText(command.keyword(), "keyword", 100),
                command.roleCode(),
                command.accountStatus(),
                managedRoleCodes
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
            AdminProvisionableAccountRoleCode roleCode,
            String keycloakUserId
    ) {
        account.setAccountId(accountId);
        account.setUserProfileId(UUID.randomUUID());
        account.setKeycloakUserId(keycloakUserId);
        account.setRoleId(roleId);
        account.setStatus(provisionedAccountPolicy.initialAccountStatus(roleCode));
        account.setFailedLoginCount(0);
        return account;
    }

    private UserProfile buildMinimalUserProfile(UUID userProfileId, String fullName) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName(fullName);
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        userProfilePolicy.initialize(userProfile);
        return userProfile;
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
        if (provisionedAccountPolicy.resolveProvisionableRole(result.role().roleCode()) == null) {
            throw new NotFoundException("Provisioned account not found");
        }
        ensureCanManageTargetRole(result.role().roleCode());
    }

    private AdminProvisionableAccountRoleCode requireProvisionableRole(AdminProvisionableAccountRoleCode roleCode) {
        return provisionedAccountPolicy.requireProvisionableRole(roleCode);
    }

    private Set<AdminProvisionableAccountRoleCode> managedTargetRolesForCurrentAccount() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        AdminProvisionableAccountRoleCode currentRoleCode =
                provisionedAccountPolicy.requireProvisionableRole(currentAccount.roleCode());
        Set<AdminProvisionableAccountRoleCode> managedRoleCodes =
                provisionedAccountPolicy.managedTargetRoles(currentRoleCode);
        if (managedRoleCodes.isEmpty()) {
            throw new AccessDeniedException("Current account is not allowed to manage provisioned accounts");
        }
        return managedRoleCodes;
    }

    private void ensureCanManageTargetRole(String targetRoleCode) {
        ensureCanManageTargetRole(provisionedAccountPolicy.requireProvisionableRole(targetRoleCode));
    }

    private void ensureCanManageTargetRole(AdminProvisionableAccountRoleCode targetRoleCode) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        AdminProvisionableAccountRoleCode currentRoleCode =
                provisionedAccountPolicy.requireProvisionableRole(currentAccount.roleCode());
        if (!provisionedAccountPolicy.canManageTargetRole(currentRoleCode, targetRoleCode)) {
            throw new AccessDeniedException("Current account is not allowed to manage target role");
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
