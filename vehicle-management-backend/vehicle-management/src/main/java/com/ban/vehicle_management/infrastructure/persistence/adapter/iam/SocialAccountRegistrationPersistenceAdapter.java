package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.SocialAccountRegistrationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountIdentity;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountIdentityPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountIdentityRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class SocialAccountRegistrationPersistenceAdapter implements SocialAccountRegistrationPortOut {

    private static final String CUSTOMER_ROLE_CODE = "CUSTOMER";

    private final AccountRepository accountRepository;
    private final AccountIdentityRepository accountIdentityRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final AccountPersistenceMapper accountPersistenceMapper;
    private final AccountIdentityPersistenceMapper accountIdentityPersistenceMapper;
    private final UserProfilePersistenceMapper userProfilePersistenceMapper;

    public SocialAccountRegistrationPersistenceAdapter(
            AccountRepository accountRepository,
            AccountIdentityRepository accountIdentityRepository,
            RoleRepository roleRepository,
            UserProfileRepository userProfileRepository,
            AccountPersistenceMapper accountPersistenceMapper,
            AccountIdentityPersistenceMapper accountIdentityPersistenceMapper,
            UserProfilePersistenceMapper userProfilePersistenceMapper
    ) {
        this.accountRepository = accountRepository;
        this.accountIdentityRepository = accountIdentityRepository;
        this.roleRepository = roleRepository;
        this.userProfileRepository = userProfileRepository;
        this.accountPersistenceMapper = accountPersistenceMapper;
        this.accountIdentityPersistenceMapper = accountIdentityPersistenceMapper;
        this.userProfilePersistenceMapper = userProfilePersistenceMapper;
    }

    @Override
    public void lockRegistration(
            SocialIdentityProvider provider,
            String providerSubject,
            String email
    ) {
        Stream.of(
                        "SOCIAL:" + provider.name() + ":" + providerSubject,
                        "EMAIL:" + email
                )
                .sorted()
                .forEach(accountRepository::lockRegistrationKey);
    }

    @Override
    public Optional<Account> findAccountByKeycloakUserId(String keycloakUserId) {
        return accountRepository.findByKeycloakUserId(keycloakUserId)
                .map(accountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<AccountIdentity> findIdentity(
            SocialIdentityProvider provider,
            String providerSubject
    ) {
        return accountIdentityRepository.findByProviderAndProviderSubject(provider, providerSubject)
                .map(accountIdentityPersistenceMapper::toDomain);
    }

    @Override
    public Optional<AccountIdentity> findIdentity(UUID accountId, SocialIdentityProvider provider) {
        return accountIdentityRepository.findByAccountIdAndProvider(accountId, provider)
                .map(accountIdentityPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }

    @Override
    public Account registerCustomer(
            Account account,
            UserProfile userProfile,
            AccountIdentity accountIdentity
    ) {
        RoleEntity customerRole = roleRepository.findByCodeAndIsActiveTrue(CUSTOMER_ROLE_CODE)
                .orElseThrow(() -> new NotFoundException("Customer role is not configured or inactive"));

        account.setRoleId(customerRole.getRoleId());
        userProfileRepository.save(userProfilePersistenceMapper.toEntity(userProfile));
        Account savedAccount = accountPersistenceMapper.toDomain(
                accountRepository.saveAndFlush(accountPersistenceMapper.toEntity(account))
        );
        accountIdentityRepository.saveAndFlush(accountIdentityPersistenceMapper.toEntity(accountIdentity));
        return savedAccount;
    }

}
