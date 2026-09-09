package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountIdentity;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountIdentityPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountIdentityEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountIdentityRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialAccountRegistrationPersistenceAdapterTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AccountIdentityRepository accountIdentityRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private AccountPersistenceMapper accountPersistenceMapper;
    @Mock private AccountIdentityPersistenceMapper accountIdentityPersistenceMapper;
    @Mock private UserProfilePersistenceMapper userProfilePersistenceMapper;
    @Mock private EntityManager entityManager;
    @Mock private Query query;

    @InjectMocks
    private SocialAccountRegistrationPersistenceAdapter adapter;

    @Test
    void shouldAcquireTransactionLocksForIdentityAndEmail() {
        when(entityManager.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(query);
        when(query.setParameter(org.mockito.ArgumentMatchers.eq("lockKey"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(query);

        adapter.lockRegistration(
                SocialIdentityProvider.GOOGLE,
                "google-sub-123",
                "customer@example.com"
        );

        verify(query).setParameter("lockKey", "EMAIL:customer@example.com");
        verify(query).setParameter("lockKey", "SOCIAL:GOOGLE:google-sub-123");
        verify(query, org.mockito.Mockito.times(2)).getSingleResult();
    }

    @Test
    void shouldPersistProfileAccountAndIdentityWithActiveCustomerRole() {
        UUID roleId = UUID.randomUUID();
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRoleId(roleId);
        Account account = new Account();
        UserProfile profile = new UserProfile();
        AccountIdentity identity = new AccountIdentity();
        AccountEntity accountEntity = new AccountEntity();
        UserProfileEntity profileEntity = new UserProfileEntity();
        AccountIdentityEntity identityEntity = new AccountIdentityEntity();

        when(roleRepository.findByCodeAndIsActiveTrue("CUSTOMER")).thenReturn(Optional.of(roleEntity));
        when(userProfilePersistenceMapper.toEntity(profile)).thenReturn(profileEntity);
        when(accountPersistenceMapper.toEntity(account)).thenReturn(accountEntity);
        when(accountRepository.saveAndFlush(accountEntity)).thenReturn(accountEntity);
        when(accountPersistenceMapper.toDomain(accountEntity)).thenReturn(account);
        when(accountIdentityPersistenceMapper.toEntity(identity)).thenReturn(identityEntity);

        Account result = adapter.registerCustomer(account, profile, identity);

        assertEquals(account, result);
        assertEquals(roleId, account.getRoleId());
        verify(userProfileRepository).save(profileEntity);
        verify(accountRepository).saveAndFlush(accountEntity);
        verify(accountIdentityRepository).saveAndFlush(identityEntity);
    }
}
