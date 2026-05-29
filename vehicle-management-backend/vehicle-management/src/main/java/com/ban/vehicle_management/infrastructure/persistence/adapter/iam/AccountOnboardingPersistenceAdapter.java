package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountOnboardingPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountOnboardingState;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountOnboardingPersistenceAdapter implements AccountOnboardingPortOut {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final CustomerRepository customerRepository;
    private final AccountPersistenceMapper accountPersistenceMapper;
    private final UserProfilePersistenceMapper userProfilePersistenceMapper;
    private final CustomerPersistenceMapper customerPersistenceMapper;

    public AccountOnboardingPersistenceAdapter(
            AccountRepository accountRepository,
            UserProfileRepository userProfileRepository,
            CustomerRepository customerRepository,
            AccountPersistenceMapper accountPersistenceMapper,
            UserProfilePersistenceMapper userProfilePersistenceMapper,
            CustomerPersistenceMapper customerPersistenceMapper
    ) {
        this.accountRepository = accountRepository;
        this.userProfileRepository = userProfileRepository;
        this.customerRepository = customerRepository;
        this.accountPersistenceMapper = accountPersistenceMapper;
        this.userProfilePersistenceMapper = userProfilePersistenceMapper;
        this.customerPersistenceMapper = customerPersistenceMapper;
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userProfileRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByIdentifyCard(String identifyCard) {
        return userProfileRepository.existsByIdentifyCard(identifyCard);
    }

    @Override
    public Optional<AccountOnboardingState> findOnboardingStateByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(accountEntity -> new AccountOnboardingState(
                        accountEntity.getAccountId(),
                        accountEntity.getUserProfileId(),
                        resolveCustomerId(accountEntity.getUserProfileId()),
                        accountEntity.getStatus()
                ));
    }

    @Override
    public Account completeOnboarding(UUID accountId, UserProfile userProfile, Customer customer) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account does not exist"));

        userProfileRepository.save(userProfilePersistenceMapper.toEntity(userProfile));

        customer.setCustomerCode(generateCustomerCode(customer.getCustomerId()));
        customerRepository.save(customerPersistenceMapper.toEntity(customer));

        accountEntity.setUserProfileId(userProfile.getUserProfileId());
        accountEntity.setStatus(AccountStatus.ACTIVE);

        return accountPersistenceMapper.toDomain(accountRepository.save(accountEntity));
    }

    private UUID resolveCustomerId(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }

        return customerRepository.findByUserProfileId(userProfileId)
                .map(CustomerEntity::getCustomerId)
                .orElse(null);
    }

    private String generateCustomerCode(UUID customerId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(customerId.toString().getBytes(StandardCharsets.UTF_8));

            String encodedDigest = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);

            return "CUS-" + encodedDigest;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
