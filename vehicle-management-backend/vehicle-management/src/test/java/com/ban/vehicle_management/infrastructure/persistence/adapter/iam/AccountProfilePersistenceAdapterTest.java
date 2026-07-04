package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.CustomerPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.EmployeePersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountProfilePersistenceAdapterTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountPersistenceMapper accountPersistenceMapper;

    @Mock
    private UserProfilePersistenceMapper userProfilePersistenceMapper;

    @Mock
    private CustomerPersistenceMapper customerPersistenceMapper;

    @Mock
    private EmployeePersistenceMapper employeePersistenceMapper;

    @InjectMocks
    private AccountProfilePersistenceAdapter adapter;

    @Test
    void completeProfileShouldUpdateExistingUserProfileWithoutReplacingAuditFields() {
        UUID accountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-20T10:00:00Z");

        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setAccountId(accountId);
        accountEntity.setUserProfileId(userProfileId);
        accountEntity.setStatus(AccountStatus.PENDING);

        UserProfileEntity existingUserProfileEntity = new UserProfileEntity();
        existingUserProfileEntity.setUserProfileId(userProfileId);
        existingUserProfileEntity.setFullName("Old Name");
        existingUserProfileEntity.setCreatedAt(createdAt);
        existingUserProfileEntity.setStatus(UserProfileStatus.ACTIVE);

        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("New Name");
        userProfile.setPhoneNumber("0901002003");
        userProfile.setDateOfBirth(LocalDate.of(1998, 3, 15));
        userProfile.setGender("MALE");
        userProfile.setAddress("Ho Chi Minh City");
        userProfile.setIdentifyCard("079100200003");
        userProfile.setStatus(UserProfileStatus.ACTIVE);

        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());
        customer.setUserProfileId(userProfileId);

        CustomerEntity customerEntity = new CustomerEntity();
        Account account = new Account();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
        when(userProfileRepository.findById(userProfileId)).thenReturn(Optional.of(existingUserProfileEntity));
        when(customerPersistenceMapper.toEntity(customer)).thenReturn(customerEntity);
        when(accountRepository.save(accountEntity)).thenReturn(accountEntity);
        when(accountPersistenceMapper.toDomain(accountEntity)).thenReturn(account);

        adapter.completeProfile(accountId, userProfile, customer);

        verify(userProfileRepository).save(existingUserProfileEntity);
        verify(userProfilePersistenceMapper, never()).toEntity(any());
        verify(customerRepository).save(customerEntity);
        assertSame(createdAt, existingUserProfileEntity.getCreatedAt());
        assertEquals("New Name", existingUserProfileEntity.getFullName());
        assertEquals("0901002003", existingUserProfileEntity.getPhoneNumber());
        assertEquals(AccountStatus.ACTIVE, accountEntity.getStatus());
    }
}
