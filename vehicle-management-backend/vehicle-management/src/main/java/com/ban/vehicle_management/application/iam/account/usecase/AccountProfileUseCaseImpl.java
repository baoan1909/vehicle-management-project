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
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.policy.AccountOnboardingPolicy;
import com.ban.vehicle_management.domain.iam.account.policy.AccountProfilePolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.employee.policy.EmployeePolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.IdentifierGenerationUtils;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountProfileUseCaseImpl implements AccountProfilePortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;
    private final CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut;
    private final InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;
    private final SystemAdminApprovalPortOut systemAdminApprovalPortOut;
    private final UserProfileAvatarPortIn userProfileAvatarPortIn;
    private final AccountProfileResultMapper accountProfileResultMapper;
    private final NotificationPortIn notificationPortIn;
    private final AccountProfilePolicy accountProfilePolicy;
    private final AccountOnboardingPolicy accountOnboardingPolicy;
    private final EmployeePolicy employeePolicy = new EmployeePolicy();
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();

    public AccountProfileUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut,
            CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut,
            SystemAdminApprovalPortOut systemAdminApprovalPortOut,
            UserProfileAvatarPortIn userProfileAvatarPortIn,
            AccountProfileResultMapper accountProfileResultMapper,
            NotificationPortIn notificationPortIn,
            AccountProfilePolicy accountProfilePolicy,
            AccountOnboardingPolicy accountOnboardingPolicy
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
        this.customerOnboardingApprovalPortOut = customerOnboardingApprovalPortOut;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
        this.systemAdminApprovalPortOut = systemAdminApprovalPortOut;
        this.userProfileAvatarPortIn = userProfileAvatarPortIn;
        this.accountProfileResultMapper = accountProfileResultMapper;
        this.notificationPortIn = notificationPortIn;
        this.accountProfilePolicy = accountProfilePolicy;
        this.accountOnboardingPolicy = accountOnboardingPolicy;
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

        CompleteAccountProfileCommand normalizedCommand = normalizeCompleteCommand(command);
        accountProfilePolicy.ensureUniqueForComplete(
                profileFieldExistsForComplete(
                        state.userProfileId(),
                        normalizedCommand.phoneNumber(),
                        true
                ),
                normalizedCommand.identifyCard() != null
                        && profileFieldExistsForComplete(
                        state.userProfileId(),
                        normalizedCommand.identifyCard(),
                        false
                )
        );

        AdminProvisionableAccountRoleCode roleCode = accountOnboardingPolicy.requireSupportedOnboardingRole(
                state.roleCode()
        );
        UUID userProfileId = state.userProfileId() == null ? UUID.randomUUID() : state.userProfileId();
        UserProfile userProfile = buildOnboardingUserProfile(userProfileId, normalizedCommand);

        Employee employee = null;
        Customer customer = null;
        Account updatedAccount;
        if (accountOnboardingPolicy.requiresEmployeeRecord(roleCode)) {
            employee = buildOnboardingEmployee(userProfileId, roleCode);
            updatedAccount = accountProfilePortOut.completeInternalProfile(accountId, userProfile, employee);
            ApprovalRequest approvalRequest = buildInternalEmployeeApprovalRequest(employee.getEmployeeId(), accountId);
            internalEmployeeApprovalPortOut.saveInternalEmployeeApprovalRequest(approvalRequest);
            notifyApprovalSubmitted(updatedAccount, approvalRequest, "Ho so nhan su da gui duyet");
            notifyApprovalReviewers(approvalRequest, "Co ho so nhan su can duyet");
        } else if (AdminProvisionableAccountRoleCode.CUSTOMER.equals(roleCode)) {
            customer = buildOnboardingCustomer(userProfileId);
            updatedAccount = accountProfilePortOut.completeProfile(accountId, userProfile, customer);
            ApprovalRequest approvalRequest = buildCustomerOnboardingApprovalRequest(customer.getCustomerId(), accountId);
            customerOnboardingApprovalPortOut.saveCustomerOnboardingApprovalRequest(approvalRequest);
            notifyApprovalSubmitted(updatedAccount, approvalRequest, "Ho so khach hang da gui duyet");
            notifyApprovalReviewers(approvalRequest, "Co ho so khach hang can duyet");
        } else {
            updatedAccount = accountProfilePortOut.completeProfileOnly(accountId, userProfile);
            if (accountOnboardingPolicy.shouldCreateSystemAdminApproval(state)) {
                ApprovalRequest approvalRequest = buildSystemAdminApprovalRequest(accountId, accountId);
                systemAdminApprovalPortOut.saveSystemAdminApprovalRequest(approvalRequest);
                notifyApprovalSubmitted(updatedAccount, approvalRequest, "Ho so quan tri he thong da gui duyet");
                notifyApprovalReviewers(approvalRequest, "Co ho so quan tri he thong can duyet");
            }
        }

        AccountProfileState refreshedState = accountProfilePortOut.findProfileStateByAccountId(updatedAccount.getAccountId())
                .orElse(null);
        if (refreshedState != null) {
            return toStatusResult(refreshedState, isOnboardingRequired(refreshedState));
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

        requireField(command, "command");
        accountProfilePolicy.ensurePatchHasAtLeastOneField(
                command.fullName(),
                command.phoneNumber(),
                command.dateOfBirth(),
                command.gender(),
                command.address(),
                command.identifyCard()
        );
        UpdateAccountProfileCommand normalizedCommand = normalizeUpdateCommand(command);
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

        userProfileAvatarPortIn.uploadAvatar(state.userProfileId(), file, accountId);
        AccountProfileState updatedState = reloadProfileState(accountId);
        return toStatusResult(updatedState, isOnboardingRequired(updatedState));
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

        userProfileAvatarPortIn.deleteAvatar(state.userProfileId());
        AccountProfileState updatedState = reloadProfileState(accountId);
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
        userProfile.setAvatarUrl(null);
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        return userProfile;
    }

    private Employee buildOnboardingEmployee(UUID userProfileId, AdminProvisionableAccountRoleCode roleCode) {
        UUID employeeId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setUserProfileId(userProfileId);
        employee.setEmployeeCode(IdentifierGenerationUtils.generateEmployeeCode(employeeId));
        employee.setJobTitle(accountOnboardingPolicy.defaultJobTitle(roleCode));
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

    private CompleteAccountProfileCommand normalizeCompleteCommand(CompleteAccountProfileCommand command) {
        requireField(command, "command");
        return new CompleteAccountProfileCommand(
                accountProfilePolicy.normalizeRequiredFullName(command.fullName()),
                accountProfilePolicy.normalizeRequiredPhoneNumber(command.phoneNumber()),
                command.dateOfBirth(),
                accountProfilePolicy.normalizeNullableGender(command.gender()),
                accountProfilePolicy.normalizeNullableAddress(command.address()),
                accountProfilePolicy.normalizeNullableIdentifyCard(command.identifyCard()),
                null
        );
    }

    private UpdateAccountProfileCommand normalizeUpdateCommand(UpdateAccountProfileCommand command) {
        return new UpdateAccountProfileCommand(
                accountProfilePolicy.normalizeNullableFullName(command.fullName()),
                accountProfilePolicy.normalizeNullablePhoneNumber(command.phoneNumber()),
                command.dateOfBirth(),
                accountProfilePolicy.normalizeNullableGender(command.gender()),
                accountProfilePolicy.normalizeNullableAddress(command.address()),
                accountProfilePolicy.normalizeNullableIdentifyCard(command.identifyCard()),
                null
        );
    }

    private boolean isOnboardingRequired(AccountProfileState state) {
        boolean latestSystemAdminApprovalRequestExists = false;
        if (accountOnboardingPolicy.needsSystemAdminApprovalLookup(state)) {
            latestSystemAdminApprovalRequestExists = systemAdminApprovalPortOut
                    .findLatestSystemAdminApprovalRequest(state.accountId())
                    .isPresent();
        }
        return accountOnboardingPolicy.isOnboardingRequired(state, latestSystemAdminApprovalRequestExists);
    }

    private boolean profileFieldExistsForComplete(UUID userProfileId, String value, boolean phoneNumber) {
        if (userProfileId == null) {
            return phoneNumber
                    ? accountProfilePortOut.existsByPhoneNumber(value)
                    : accountProfilePortOut.existsByIdentifyCard(value);
        }
        return phoneNumber
                ? accountProfilePortOut.existsByPhoneNumberAndUserProfileIdNot(value, userProfileId)
                : accountProfilePortOut.existsByIdentifyCardAndUserProfileIdNot(value, userProfileId);
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

    private AccountProfileStatusResult toStatusResult(AccountProfileState state, boolean onboardingRequired) {
        return resolvePublicAvatarUrl(accountProfileResultMapper.toStatusResult(state, onboardingRequired));
    }

    private AccountProfileStatusResult resolvePublicAvatarUrl(AccountProfileStatusResult result) {
        if (result == null || result.profile() == null || result.profile().userProfileId() == null) {
            return result;
        }
        UserProfile profileToResolve = new UserProfile();
        profileToResolve.setUserProfileId(result.profile().userProfileId());
        profileToResolve.setAvatarUrl(result.profile().avatarUrl());
        UserProfile resolvedProfile = userProfileAvatarPortIn.withResolvedAvatarUrl(profileToResolve);
        if (resolvedProfile == null) {
            return result;
        }
        String resolvedAvatarUrl = resolvedProfile.getAvatarUrl();
        if (Objects.equals(result.profile().avatarUrl(), resolvedAvatarUrl)) {
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

    private AccountProfileState reloadProfileState(UUID accountId) {
        return accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private void notifyApprovalSubmitted(Account account, ApprovalRequest approvalRequest, String title) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendWebNotification(new SendNotificationCommand(
                account.getAccountId(),
                title,
                "Ho so cua ban da duoc gui den nhom duyet.",
                approvalRequest.getTargetSchema(),
                approvalRequest.getTargetTable(),
                approvalRequest.getTargetId()
        ));
    }

    private void notifyApprovalReviewers(ApprovalRequest approvalRequest, String title) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                false,
                NotificationAudience.APPROVERS,
                null,
                title,
                "Co yeu cau phe duyet moi can xu ly.",
                approvalRequest.getTargetSchema(),
                approvalRequest.getTargetTable(),
                approvalRequest.getTargetId()
        ));
    }
}
