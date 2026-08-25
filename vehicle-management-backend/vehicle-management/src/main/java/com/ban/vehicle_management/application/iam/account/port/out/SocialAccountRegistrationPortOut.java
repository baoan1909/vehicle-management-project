package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountIdentity;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import java.util.Optional;
import java.util.UUID;

public interface SocialAccountRegistrationPortOut {

    void lockRegistration(SocialIdentityProvider provider, String providerSubject, String email);

    Optional<Account> findAccountByKeycloakUserId(String keycloakUserId);

    Optional<AccountIdentity> findIdentity(SocialIdentityProvider provider, String providerSubject);

    Optional<AccountIdentity> findIdentity(UUID accountId, SocialIdentityProvider provider);

    boolean existsByEmail(String email);

    Account registerCustomer(Account account, UserProfile userProfile, AccountIdentity accountIdentity);
}
