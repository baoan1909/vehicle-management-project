package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.mapper.AccountProfileResultMapper;
import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.command.UpdateAccountProfileCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountProfileStatusResult;
import com.ban.vehicle_management.application.iam.account.port.in.AccountProfilePortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.iam.account.policy.AccountProfilePolicy;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountProfileUseCaseImpl implements AccountProfilePortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountProfilePortOut accountProfilePortOut;
    private final AccountProfileResultMapper accountProfileResultMapper;
    private final AccountProfilePolicy accountProfilePolicy;

    public AccountProfileUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            AccountProfilePortOut accountProfilePortOut,
            AccountProfileResultMapper accountProfileResultMapper,
            AccountProfilePolicy accountProfilePolicy
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountProfilePortOut = accountProfilePortOut;
        this.accountProfileResultMapper = accountProfileResultMapper;
        this.accountProfilePolicy = accountProfilePolicy;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountProfileStatusResult getMyProfile() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        AccountProfileState state = accountProfilePortOut.findProfileStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));

        return accountProfileResultMapper.toStatusResult(state, isOnboardingRequired(state));
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

        UUID userProfileId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName(normalizedCommand.fullName());
        userProfile.setPhoneNumber(normalizedCommand.phoneNumber());
        userProfile.setDateOfBirth(normalizedCommand.dateOfBirth());
        userProfile.setGender(normalizedCommand.gender());
        userProfile.setAddress(normalizedCommand.address());
        userProfile.setIdentifyCard(normalizedCommand.identifyCard());
        userProfile.setAvatarUrl(normalizedCommand.avatarUrl());
        userProfile.setStatus(UserProfileStatus.ACTIVE);

        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setUserProfileId(userProfileId);
        customer.setCustomerType(CustomerType.REGISTERED);
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.PENDING);

        Account updatedAccount = accountProfilePortOut.completeProfile(accountId, userProfile, customer);
        AccountProfileState refreshedState = accountProfilePortOut
                .findProfileStateByAccountId(updatedAccount.getAccountId())
                .orElse(null);

        if (refreshedState != null) {
            return accountProfileResultMapper.toStatusResult(refreshedState, false);
        }

        return accountProfileResultMapper.toStatusResult(
                updatedAccount,
                userProfile,
                customer,
                userProfileId,
                customerId
        );
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
        return accountProfileResultMapper.toStatusResult(updatedState, isOnboardingRequired(updatedState));
    }

    private boolean isOnboardingRequired(AccountProfileState state) {
        return state.userProfileId() == null || state.customerId() == null;
    }

}
