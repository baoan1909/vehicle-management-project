package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.infrastructure.mapper.iam.AccountPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AccountRegistrationPersistenceAdapter implements AccountRegistrationPortOut {

    private static final String CUSTOMER_ROLE_CODE = "CUSTOMER";

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AccountPersistenceMapper accountPersistenceMapper;

    public AccountRegistrationPersistenceAdapter(
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            AccountPersistenceMapper accountPersistenceMapper
    ) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.accountPersistenceMapper = accountPersistenceMapper;
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
    public Optional<String> findKeycloakUserIdByEmail(String email) {
        return accountRepository.findByEmail(email)
                .map(account -> account.getKeycloakUserId())
                .filter(keycloakUserId -> keycloakUserId != null && !keycloakUserId.isBlank());
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email)
                .map(accountPersistenceMapper::toDomain);
    }

    @Override
    public Account registerAccount(RegisterAccountCommand command, String keycloakUserId) {
        RoleEntity customerRole = roleRepository.findByCode(CUSTOMER_ROLE_CODE)
                .orElseThrow(() -> new NotFoundException("Customer role is not configured"));

        UUID accountId = UUID.randomUUID();

        Account account = new Account();
        account.setAccountId(accountId);
        account.setUserProfileId(null);
        account.setKeycloakUserId(keycloakUserId);
        account.setUsername(command.username());
        account.setEmail(command.email());
        account.setRoleId(customerRole.getRoleId());
        account.setStatus(AccountStatus.PENDING);
        account.setFailedLoginCount(0);
        return accountPersistenceMapper.toDomain(accountRepository.save(accountPersistenceMapper.toEntity(account)));
    }
}
