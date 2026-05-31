package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface AccountProfilePortOut {

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByIdentifyCard(String identifyCard);

    Optional<AccountProfileState> findProfileStateByAccountId(UUID accountId);

    Account completeProfile(UUID accountId, UserProfile userProfile, Customer customer);

    boolean existsByPhoneNumberAndUserProfileIdNot(String phoneNumber, UUID userProfileId);

    boolean existsByIdentifyCardAndUserProfileIdNot(String identifyCard, UUID userProfileId);

    AccountProfileState updateProfile(UUID accountId, UserProfile userProfile);
}
