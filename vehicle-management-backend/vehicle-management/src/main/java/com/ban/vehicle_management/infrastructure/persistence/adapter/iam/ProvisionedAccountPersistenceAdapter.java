package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.model.command.ProvisionedAccountFilterCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.application.iam.account.port.out.ProvisionedAccountPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountStatusHistory;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountStatusHistoryPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.iam.ProvisionedAccountReadModelMapper;
import com.ban.vehicle_management.infrastructure.mapper.people.UserProfilePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountStatusHistoryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.EmployeeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountStatusHistoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.iam.ProvisionedAccountSpecifications;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ProvisionedAccountPersistenceAdapter implements ProvisionedAccountPortOut {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final AccountStatusHistoryRepository accountStatusHistoryRepository;
    private final AccountPersistenceMapper accountPersistenceMapper;
    private final AccountStatusHistoryPersistenceMapper accountStatusHistoryPersistenceMapper;
    private final ProvisionedAccountReadModelMapper provisionedAccountReadModelMapper;
    private final UserProfilePersistenceMapper userProfilePersistenceMapper;
    private final EntityManager entityManager;

    public ProvisionedAccountPersistenceAdapter(
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            UserProfileRepository userProfileRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            AccountStatusHistoryRepository accountStatusHistoryRepository,
            AccountPersistenceMapper accountPersistenceMapper,
            AccountStatusHistoryPersistenceMapper accountStatusHistoryPersistenceMapper,
            ProvisionedAccountReadModelMapper provisionedAccountReadModelMapper,
            UserProfilePersistenceMapper userProfilePersistenceMapper,
            EntityManager entityManager
    ) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.userProfileRepository = userProfileRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.accountStatusHistoryRepository = accountStatusHistoryRepository;
        this.accountPersistenceMapper = accountPersistenceMapper;
        this.accountStatusHistoryPersistenceMapper = accountStatusHistoryPersistenceMapper;
        this.provisionedAccountReadModelMapper = provisionedAccountReadModelMapper;
        this.userProfilePersistenceMapper = userProfilePersistenceMapper;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsByUsername(String username) {
        return accountRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accountRepository.existsByEmail(email);
    }

    @Override
    public UUID findActiveRoleIdByCode(AdminProvisionableAccountRoleCode roleCode) {
        return roleRepository.findByCodeAndIsActiveTrue(roleCode.name())
                .orElseThrow(() -> new NotFoundException("Role " + roleCode.name() + " is not configured"))
                .getRoleId();
    }

    @Override
    public void provisionAccount(Account account, UserProfile userProfile) {
        userProfileRepository.save(userProfilePersistenceMapper.toEntity(userProfile));
        accountRepository.save(accountPersistenceMapper.toEntity(account));
        flushAndClear();
    }

    @Override
    public List<ProvisionedAccountResult> findProvisionedAccounts(ProvisionedAccountFilterCommand command) {
        Specification<AccountEntity> specification = ProvisionedAccountSpecifications.withFilters(command);
        return accountRepository.findAll(specification).stream()
                .map(provisionedAccountReadModelMapper::toSummaryResult)
                .toList();
    }

    @Override
    public Optional<ProvisionedAccountResult> findProvisionedAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(provisionedAccountReadModelMapper::toResult);
    }

    @Override
    public void updateProvisionedAccountStatus(
            UUID accountId,
            AccountStatus accountStatus,
            UserProfileStatus userProfileStatus,
            CustomerStatus customerStatus,
            EmployeeStatus employeeStatus,
            UUID changedBy,
            String reason
    ) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Provisioned account not found"));
        UserProfileEntity userProfileEntity = resolveUserProfile(accountEntity.getUserProfileId());
        CustomerEntity customerEntity = resolveCustomer(accountEntity.getUserProfileId());
        EmployeeEntity employeeEntity = resolveEmployee(accountEntity.getUserProfileId());

        AccountStatus previousStatus = accountEntity.getStatus();
        accountEntity.setStatus(accountStatus);
        if (userProfileEntity != null) {
            userProfileEntity.setStatus(userProfileStatus);
        }
        if (customerEntity != null) {
            customerEntity.setStatus(customerStatus);
        }
        if (employeeEntity != null && employeeStatus != null) {
            employeeEntity.setStatus(employeeStatus);
        }

        if (!previousStatus.equals(accountStatus)) {
            AccountStatusHistory statusHistory = new AccountStatusHistory(
                    UUID.randomUUID(),
                    accountId,
                    previousStatus,
                    accountStatus,
                    reason,
                    Instant.now(),
                    changedBy
            );
            AccountStatusHistoryEntity statusHistoryEntity =
                    accountStatusHistoryPersistenceMapper.toEntity(statusHistory);
            accountStatusHistoryRepository.save(statusHistoryEntity);
        }

        accountRepository.save(accountEntity);
        if (userProfileEntity != null) {
            userProfileRepository.save(userProfileEntity);
        }
        if (customerEntity != null) {
            customerRepository.save(customerEntity);
        }
        if (employeeEntity != null && employeeStatus != null) {
            employeeRepository.save(employeeEntity);
        }
        flushAndClear();
    }

    @Override
    public void updateProvisionedAccountRole(UUID accountId, UUID roleId) {
        AccountEntity accountEntity = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Provisioned account not found"));
        accountEntity.setRoleId(roleId);
        accountRepository.save(accountEntity);
        flushAndClear();
    }

    private void flushAndClear() {
        accountRepository.flush();
        entityManager.clear();
    }

    private UserProfileEntity resolveUserProfile(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return userProfileRepository.findById(userProfileId).orElse(null);
    }

    private EmployeeEntity resolveEmployee(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return employeeRepository.findByUserProfileId(userProfileId).orElse(null);
    }

    private CustomerEntity resolveCustomer(UUID userProfileId) {
        if (userProfileId == null) {
            return null;
        }
        return customerRepository.findByUserProfileId(userProfileId).orElse(null);
    }
}
