package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountStatusHistoryPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.iam.ProvisionedAccountReadModelMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountStatusHistoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvisionedAccountPersistenceAdapterTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountStatusHistoryRepository accountStatusHistoryRepository;

    @Mock
    private AccountPersistenceMapper accountPersistenceMapper;

    @Mock
    private AccountStatusHistoryPersistenceMapper accountStatusHistoryPersistenceMapper;

    @Mock
    private ProvisionedAccountReadModelMapper provisionedAccountReadModelMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ProvisionedAccountPersistenceAdapter adapter;

    @Test
    void shouldClearPersistenceContextAfterProvisionAccount() {
        Account account = new Account();
        AccountEntity accountEntity = new AccountEntity();
        when(accountPersistenceMapper.toEntity(account)).thenReturn(accountEntity);

        adapter.provisionAccount(account);

        verify(accountRepository).save(accountEntity);
        verify(accountRepository).flush();
        verify(entityManager).clear();
    }

    @Test
    void shouldClearPersistenceContextAfterUpdatingProvisionedAccountRole() {
        UUID accountId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        AccountEntity accountEntity = new AccountEntity();
        when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(accountEntity));

        adapter.updateProvisionedAccountRole(accountId, roleId);

        verify(accountRepository).save(accountEntity);
        verify(accountRepository).flush();
        verify(entityManager).clear();
    }
}
