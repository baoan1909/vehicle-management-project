package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.CompleteAccountOnboardingCommand;
import com.ban.vehicle_management.application.iam.account.model.result.AccountOnboardingStatusResult;
import com.ban.vehicle_management.application.iam.account.model.result.CompleteAccountOnboardingResult;
import com.ban.vehicle_management.application.iam.account.port.in.AccountOnboardingPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPort;
import com.ban.vehicle_management.application.iam.account.port.out.AccountOnboardingPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountOnboardingState;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountOnboardingUseCaseImpl implements AccountOnboardingPortIn {

    private final CurrentAccountPort currentAccountPort;
    private final AccountOnboardingPortOut accountOnboardingPortOut;

    public AccountOnboardingUseCaseImpl(
            CurrentAccountPort currentAccountPort,
            AccountOnboardingPortOut accountOnboardingPortOut
    ) {
        this.currentAccountPort = currentAccountPort;
        this.accountOnboardingPortOut = accountOnboardingPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountOnboardingStatusResult getMyOnboardingStatus() {
        UUID accountId = currentAccountPort.getCurrentAccountIdOrThrow();
        AccountOnboardingState state = accountOnboardingPortOut.findOnboardingStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));

        return new AccountOnboardingStatusResult(
                state.accountId(),
                state.accountStatus().name(),
                isOnboardingRequired(state),
                state.userProfileId(),
                state.customerId()
        );
    }

    @Override
    @Transactional
    public CompleteAccountOnboardingResult completeMyOnboarding(CompleteAccountOnboardingCommand command) {
        UUID accountId = currentAccountPort.getCurrentAccountIdOrThrow();
        AccountOnboardingState state = accountOnboardingPortOut.findOnboardingStateByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Current account does not exist"));

        if (!isOnboardingRequired(state)) {
            throw new ConflictException("Onboarding is already completed");
        }

        CompleteAccountOnboardingCommand normalizedCommand = normalize(command);
        ensureNoConflict(normalizedCommand);

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

        Account updatedAccount = accountOnboardingPortOut.completeOnboarding(accountId, userProfile, customer);

        return new CompleteAccountOnboardingResult(
                updatedAccount.getAccountId(),
                userProfileId,
                customerId,
                updatedAccount.getStatus().name(),
                false
        );
    }

    private CompleteAccountOnboardingCommand normalize(CompleteAccountOnboardingCommand command) {
        String fullName = TextValidationUtils.normalizeRequiredText(command.fullName(), "fullName", 255);
        String phoneNumber = normalizeRequiredPhoneNumber(command.phoneNumber());
        String gender = TextValidationUtils.normalizeNullableText(command.gender(), "gender", 20);
        String address = TextValidationUtils.normalizeNullableText(command.address(), "address", 255);
        String identifyCard = TextValidationUtils.normalizeAlphaNumeric(command.identifyCard(), "identifyCard", 50);
        String avatarUrl = TextValidationUtils.normalizeNullableText(command.avatarUrl(), "avatarUrl", 500);

        return new CompleteAccountOnboardingCommand(
                fullName,
                phoneNumber,
                command.dateOfBirth(),
                gender,
                address,
                identifyCard,
                avatarUrl
        );
    }

    private String normalizeRequiredPhoneNumber(String phoneNumber) {
        String normalizedPhoneNumber = TextValidationUtils.normalizePhoneNumber(phoneNumber, "phoneNumber", 20);
        if (normalizedPhoneNumber == null) {
            throw new BadRequestException("phoneNumber must not be blank");
        }
        return normalizedPhoneNumber;
    }

    private void ensureNoConflict(CompleteAccountOnboardingCommand command) {
        if (accountOnboardingPortOut.existsByPhoneNumber(command.phoneNumber())) {
            throw new ConflictException("Phone number already exists");
        }

        if (command.identifyCard() != null
                && accountOnboardingPortOut.existsByIdentifyCard(command.identifyCard())) {
            throw new ConflictException("Identify card already exists");
        }
    }

    private boolean isOnboardingRequired(AccountOnboardingState state) {
        return state.userProfileId() == null || state.customerId() == null;
    }
}
