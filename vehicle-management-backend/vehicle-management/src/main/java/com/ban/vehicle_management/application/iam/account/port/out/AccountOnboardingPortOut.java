package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountOnboardingState;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface AccountOnboardingPortOut {

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByIdentifyCard(String identifyCard);

    Optional<AccountOnboardingState> findOnboardingStateByAccountId(UUID accountId);

    Account completeOnboarding(UUID accountId, UserProfile userProfile, Customer customer);
}
