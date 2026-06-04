package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountProfileState;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.IdentifierGenerationUtils;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountProfilePersistenceAdapter implements AccountProfilePortOut {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final CustomerRepository customerRepository;
    private final AccountPersistenceMapper accountPersistenceMapper;
    private final UserProfilePersistenceMapper userProfilePersistenceMapper;
    private final CustomerPersistenceMapper customerPersistenceMapper;

    public AccountProfilePersistenceAdapter(
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
    public boolean existsByPhoneNumberAndUserProfileIdNot(String phoneNumber, UUID userProfileId) {
        return userProfileRepository.existsByPhoneNumberAndUserProfileIdNot(phoneNumber, userProfileId);
    }

    @Override
    public boolean existsByIdentifyCardAndUserProfileIdNot(String identifyCard, UUID userProfileId) {
        return userProfileRepository.existsByIdentifyCardAndUserProfileIdNot(identifyCard, userProfileId);
    }

    @Override
    public Optional<AccountProfileState> findProfileStateByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(this::toProfileState);
    }

    @Override
    public Account completeProfile(UUID accountId, UserProfile userProfile, Customer customer) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account does not exist"));

        userProfileRepository.save(userProfilePersistenceMapper.toEntity(userProfile));

        customer.setCustomerCode(IdentifierGenerationUtils.generateCustomerCode(customer.getCustomerId()));
        customerRepository.save(customerPersistenceMapper.toEntity(customer));

        accountEntity.setUserProfileId(userProfile.getUserProfileId());
        accountEntity.setStatus(AccountStatus.ACTIVE);

        return accountPersistenceMapper.toDomain(accountRepository.save(accountEntity));
    }

    @Override
    public AccountProfileState updateProfile(UUID accountId, UserProfile userProfile) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account does not exist"));

        UUID userProfileId = accountEntity.getUserProfileId();
        if (userProfileId == null) {
            throw new NotFoundException("User profile does not exist");
        }

        UserProfileEntity existingUserProfileEntity = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new NotFoundException("User profile does not exist"));

        existingUserProfileEntity.setFullName(userProfile.getFullName());
        existingUserProfileEntity.setPhoneNumber(userProfile.getPhoneNumber());
        existingUserProfileEntity.setDateOfBirth(userProfile.getDateOfBirth());
        existingUserProfileEntity.setGender(userProfile.getGender());
        existingUserProfileEntity.setAddress(userProfile.getAddress());
        existingUserProfileEntity.setIdentifyCard(userProfile.getIdentifyCard());
        existingUserProfileEntity.setAvatarUrl(userProfile.getAvatarUrl());
        existingUserProfileEntity.setStatus(userProfile.getStatus());

        userProfileRepository.save(existingUserProfileEntity);
        return toProfileState(accountEntity);
    }

    private AccountProfileState toProfileState(AccountEntity accountEntity) {
        UUID userProfileId = accountEntity.getUserProfileId();
        UserProfileEntity userProfileEntity = resolveUserProfile(userProfileId);
        CustomerEntity customerEntity = resolveCustomer(userProfileId);

        return new AccountProfileState(
                accountEntity.getAccountId(),
                accountEntity.getUsername(),
                accountEntity.getEmail(),
                accountEntity.getKeycloakUserId(),
                userProfileId,
                userProfileEntity == null ? null : userProfileEntity.getFullName(),
                userProfileEntity == null ? null : userProfileEntity.getDateOfBirth(),
                userProfileEntity == null ? null : userProfileEntity.getGender(),
                userProfileEntity == null ? null : userProfileEntity.getPhoneNumber(),
                userProfileEntity == null ? null : userProfileEntity.getAddress(),
                userProfileEntity == null ? null : userProfileEntity.getIdentifyCard(),
                userProfileEntity == null ? null : userProfileEntity.getAvatarUrl(),
                userProfileEntity == null ? null : userProfileEntity.getStatus(),
                customerEntity == null ? null : customerEntity.getCustomerId(),
                customerEntity == null ? null : customerEntity.getCustomerCode(),
                customerEntity == null ? null : customerEntity.getCustomerType(),
                customerEntity == null ? null : customerEntity.getStatus(),
                customerEntity == null ? null : customerEntity.getApprovalStatus(),
                accountEntity.getStatus()
        );
    }

    private UserProfileEntity resolveUserProfile(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return userProfileRepository.findById(userProfileId).orElse(null);
    }

    private CustomerEntity resolveCustomer(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return customerRepository.findByUserProfileId(userProfileId).orElse(null);
    }
}
