package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.mapper.AccountProfileResultMapper;
import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.application.iam.account.port.in.AccountProfilePortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.CustomerOnboardingApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.InternalEmployeeApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.authorization.SystemAdminApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.CustomerOnboardingApprovalPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SystemAdminApprovalPortOut;
import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;
import com.ban.vehicle_management.application.storage.port.out.FileStoragePort;
import com.ban.vehicle_management.application.storage.service.StorageUrlResolver;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.iam.account.policy.AccountProfilePolicy;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.employee.policy.EmployeePolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.storage.StorageBucket;
import com.ban.vehicle_management.shared.enumeration.storage.StorageFolder;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.IdentifierGenerationUtils;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountProfileUseCaseImpl implements AccountProfilePortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountProfileUseCaseImpl.class);
    private static final String USER_PROFILE_RESOURCE_TYPE = "people.user_profiles";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;
    private final CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut;
    private final InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;
    private final SystemAdminApprovalPortOut systemAdminApprovalPortOut;
    private final FileStoragePort fileStoragePort;
    private final StorageUrlResolver storageUrlResolver;
    private final AccountProfileResultMapper accountProfileResultMapper;
    private final AccountProfilePolicy accountProfilePolicy;
    private final EmployeePolicy employeePolicy = new EmployeePolicy();
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();

    public AccountProfileUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut,
            CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut,
            SystemAdminApprovalPortOut systemAdminApprovalPortOut,
            FileStoragePort fileStoragePort,
            StorageUrlResolver storageUrlResolver,
            AccountProfileResultMapper accountProfileResultMapper,
            AccountProfilePolicy accountProfilePolicy
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
        this.customerOnboardingApprovalPortOut = customerOnboardingApprovalPortOut;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
        this.systemAdminApprovalPortOut = systemAdminApprovalPortOut;
        this.fileStoragePort = fileStoragePort;
        this.storageUrlResolver = storageUrlResolver;
        this.accountProfileResultMapper = accountProfileResultMapper;
        this.accountProfilePolicy = accountProfilePolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountProfileStatusResult getMyProfile() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        AccountProfileState state = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));

        return toStatusResult(state, isOnboardingRequired(state));
    }

    @Override
    @Transactional
    public AccountProfileStatusResult completeMyProfile(CompleteAccountProfileCommand command) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        AccountProfileState state = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));

        if (!isOnboardingRequired(state)) {
            throw new ConflictException("Onboarding is already completed");
        }

        CompleteAccountProfileCommand normalizedCommand = accountProfilePolicy.normalizeForComplete(command);
        accountProfilePolicy.ensureUniqueForComplete(
                accountProfilePortOut.existsByPhoneNumber(normalizedCommand.phoneNumber()),
                normalizedCommand.identifyCard() != null
                        && accountProfilePortOut.existsByIdentifyCard(normalizedCommand.identifyCard())
        );

        AdminProvisionableAccountRoleCode roleCode = requireSupportedOnboardingRole(state.roleCode());
        UUID userProfileId = UUID.randomUUID();
        UserProfile userProfile = buildOnboardingUserProfile(userProfileId, normalizedCommand);

        Employee employee = null;
        Customer customer = null;
        Account updatedAccount;
        if (requiresEmployeeRecord(roleCode)) {
            employee = buildOnboardingEmployee(userProfileId, roleCode);
            updatedAccount = accountProfilePortOut.completeInternalProfile(accountId, userProfile, employee);
            internalEmployeeApprovalPortOut.saveInternalEmployeeApprovalRequest(
                    buildInternalEmployeeApprovalRequest(employee.getEmployeeId(), accountId)
            );
        } else if (AdminProvisionableAccountRoleCode.CUSTOMER.equals(roleCode)) {
            customer = buildOnboardingCustomer(userProfileId);
            updatedAccount = accountProfilePortOut.completeProfile(accountId, userProfile, customer);
            customerOnboardingApprovalPortOut.saveCustomerOnboardingApprovalRequest(
                    buildCustomerOnboardingApprovalRequest(customer.getCustomerId(), accountId)
            );
        } else {
            updatedAccount = accountProfilePortOut.completeProfileOnly(accountId, userProfile);
            if (shouldCreateSystemAdminApproval(state)) {
                systemAdminApprovalPortOut.saveSystemAdminApprovalRequest(
                        buildSystemAdminApprovalRequest(accountId, accountId)
                );
            }
        }

        AccountProfileState refreshedState = accountProfilePortOut.findProfileStateByAccountId(updatedAccount.getAccountId())
                .orElse(null);
        if (refreshedState != null) {
            return toStatusResult(refreshedState, false);
        }

        return resolvePublicAvatarUrl(accountProfileResultMapper.toStatusResult(
                updatedAccount,
                userProfile,
                employee,
                customer,
                userProfileId,
                employee == null ? null : employee.getEmployeeId(),
                customer == null ? null : customer.getCustomerId()
        ));
    }

    @Override
    @Transactional
    public AccountProfileStatusResult updateMyProfile(UpdateAccountProfileCommand command) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        AccountProfileState state = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));

        if (state.userProfileId() == null) {
            throw new ConflictException("Profile is not ready. Complete onboarding first.");
        }

        accountProfilePolicy.ensurePatchHasAtLeastOneField(command);
        UpdateAccountProfileCommand normalizedCommand = accountProfilePolicy.normalizeForUpdate(command);
        UserProfile updatedProfile = accountProfileResultMapper.mergeProfile(state, normalizedCommand);
        accountProfilePolicy.validateRequiredProfileFields(updatedProfile);
        accountProfilePolicy.ensureUniqueForUpdate(
                updatedProfile.getPhoneNumber() != null
                        && accountProfilePortOut.existsByPhoneNumberAndUserProfileIdNot(
                        updatedProfile.getPhoneNumber(),
                        state.userProfileId()
                ),
                updatedProfile.getIdentifyCard() != null
                        && accountProfilePortOut.existsByIdentifyCardAndUserProfileIdNot(
                        updatedProfile.getIdentifyCard(),
                        state.userProfileId()
                )
        );

        AccountProfileState updatedState = accountProfilePortOut.updateProfile(accountId, updatedProfile);
        return toStatusResult(updatedState, isOnboardingRequired(updatedState));
    }

    @Override
    @Transactional
    public AccountProfileStatusResult uploadMyAvatar(MultipartFile file) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        AccountProfileState state = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));

        if (state.userProfileId() == null) {
            throw new ConflictException("Profile is not ready. Complete onboarding first.");
        }

        StoredFile storedFile = fileStoragePort.store(new StoreFileCommand(
                file,
                StorageBucket.PUBLIC,
                StorageFolder.AVATAR,
                USER_PROFILE_RESOURCE_TYPE,
                state.userProfileId(),
                accountId,
                Map.of("account_id", accountId.toString())
        ));

        try {
            AccountProfileState updatedState = accountProfilePortOut.updateAvatar(accountId, storedFile.objectKey());
            deleteManagedAvatarAfterCommit(state.avatarUrl());
            return toStatusResult(updatedState, isOnboardingRequired(updatedState));
        } catch (RuntimeException exception) {
            deleteQuietly(storedFile.objectKey());
            throw exception;
        }
    }

    @Override
    @Transactional
    public AccountProfileStatusResult deleteMyAvatar() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        AccountProfileState state = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));

        if (state.userProfileId() == null) {
            throw new ConflictException("Profile is not ready. Complete onboarding first.");
        }

        AccountProfileState updatedState = accountProfilePortOut.updateAvatar(accountId, null);
        deleteManagedAvatarAfterCommit(state.avatarUrl());
        return toStatusResult(updatedState, isOnboardingRequired(updatedState));
    }

    private UserProfile buildOnboardingUserProfile(UUID userProfileId, CompleteAccountProfileCommand command) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName(command.fullName());
        userProfile.setPhoneNumber(command.phoneNumber());
        userProfile.setDateOfBirth(command.dateOfBirth());
        userProfile.setGender(command.gender());
        userProfile.setAddress(command.address());
        userProfile.setIdentifyCard(command.identifyCard());
        userProfile.setAvatarUrl(command.avatarUrl());
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        return userProfile;
    }

    private Employee buildOnboardingEmployee(UUID userProfileId, AdminProvisionableAccountRoleCode roleCode) {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setUserProfileId(userProfileId);
        employee.setEmployeeCode(IdentifierGenerationUtils.generateEmployeeCode(employeeId));
        employee.setJobTitle(defaultJobTitle(roleCode));
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeePolicy.initialize(employee);
        return employee;
    }

    private Customer buildOnboardingCustomer(UUID userProfileId) {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setUserProfileId(userProfileId);
        customer.setCustomerType(CustomerType.REGISTERED);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.PENDING);
        return customer;
    }

    private boolean isOnboardingRequired(AccountProfileState state) {
        AdminProvisionableAccountRoleCode roleCode = resolveProvisionableRole(state.roleCode());
        if (roleCode == null) {
            return state.userProfileId() == null;
        }
        if (requiresEmployeeRecord(roleCode)) {
            return state.userProfileId() == null || state.employeeId() == null;
        }
        if (AdminProvisionableAccountRoleCode.CUSTOMER.equals(roleCode)) {
            return state.userProfileId() == null || state.customerId() == null;
        }
        return state.userProfileId() == null;
    }

    private boolean requiresEmployeeRecord(AdminProvisionableAccountRoleCode roleCode) {
        return roleCode != null && roleCode.requiresEmployeeRecord();
    }

    private boolean shouldCreateSystemAdminApproval(AccountProfileState state) {
        return AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.name().equals(state.roleCode())
                && AccountStatus.PENDING.equals(state.accountStatus());
    }

    private String defaultJobTitle(AdminProvisionableAccountRoleCode roleCode) {
        return switch (roleCode) {
            case EMPLOYEE -> "Parking Staff";
            case PARKING_MANAGER -> "Parking Manager";
            case CUSTOMER, SYSTEM_ADMIN -> null;
        };
    }

    private AdminProvisionableAccountRoleCode requireSupportedOnboardingRole(String roleCode) {
        AdminProvisionableAccountRoleCode resolvedRole = resolveProvisionableRole(roleCode);
        if (resolvedRole == null) {
            throw new BadRequestException("Current account role is not supported for onboarding");
        }
        return resolvedRole;
    }

    private AdminProvisionableAccountRoleCode resolveProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private ApprovalRequest buildInternalEmployeeApprovalRequest(UUID employeeId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setRequestType(InternalEmployeeApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(InternalEmployeeApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(InternalEmployeeApprovalAccessGuard.TARGET_TABLE);
        approvalRequest.setTargetId(employeeId);
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequestPolicy.initialize(approvalRequest);
        return approvalRequest;
    }

    private ApprovalRequest buildCustomerOnboardingApprovalRequest(UUID customerId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setRequestType(CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(CustomerOnboardingApprovalAccessGuard.TARGET_TABLE);
        approvalRequest.setTargetId(customerId);
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequestPolicy.initialize(approvalRequest);
        return approvalRequest;
    }

    private ApprovalRequest buildSystemAdminApprovalRequest(UUID accountId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setRequestType(SystemAdminApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(SystemAdminApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(SystemAdminApprovalAccessGuard.TARGET_TABLE);
        approvalRequest.setTargetId(accountId);
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequestPolicy.initialize(approvalRequest);
        return approvalRequest;
    }

    private void deleteManagedAvatarAfterCommit(String objectKey) {
        if (!storageUrlResolver.isManagedAvatarObjectKey(objectKey)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(objectKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(objectKey);
            }
        });
    }

    private AccountProfileStatusResult toStatusResult(AccountProfileState state, boolean onboardingRequired) {
        return resolvePublicAvatarUrl(accountProfileResultMapper.toStatusResult(state, onboardingRequired));
    }

    private AccountProfileStatusResult resolvePublicAvatarUrl(AccountProfileStatusResult result) {
        if (result == null || result.profile() == null || result.profile().avatarUrl() == null) {
            return result;
        }
        String avatarUrl = result.profile().avatarUrl();
        if (!storageUrlResolver.isManagedAvatarObjectKey(avatarUrl)) {
            return result;
        }

        String resolvedAvatarUrl = storageUrlResolver.resolvePublicAvatarUrl(avatarUrl);
        if (avatarUrl.equals(resolvedAvatarUrl)) {
            return result;
        }

        AccountProfileStatusResult.ProfileInfoResult profile = result.profile();
        return new AccountProfileStatusResult(
                result.onboardingRequired(),
                result.account(),
                new AccountProfileStatusResult.ProfileInfoResult(
                        profile.userProfileId(),
                        profile.fullName(),
                        profile.dateOfBirth(),
                        profile.gender(),
                        profile.phoneNumber(),
                        profile.address(),
                        profile.identifyCard(),
                        resolvedAvatarUrl,
                        profile.userProfileStatus()
                ),
                result.employee(),
                result.customer()
        );
    }

    private void deleteQuietly(String objectKey) {
        try {
            fileStoragePort.delete(objectKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to delete avatar object {}", objectKey, exception);
        }
    }
}
