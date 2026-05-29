package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPort;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RolePermissionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class AccountAuthorizationPersistenceAdapter implements AccountAuthorizationPort {

    private final AccountRepository accountRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public AccountAuthorizationPersistenceAdapter(
            AccountRepository accountRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.accountRepository = accountRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public Optional<CurrentAccountAccess> findByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(this::toCurrentAccountAccess);
    }

    @Override
    public Optional<CurrentAccountAccess> findByKeycloakUserId(String keycloakUserId) {
        return accountRepository.findByKeycloakUserId(keycloakUserId)
                .map(this::toCurrentAccountAccess);
    }

    @Override
    public Set<String> findPermissionCodesByAccountId(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(AccountEntity::getRoleId)
                .map(rolePermissionRepository::findActivePermissionCodesByRoleId)
                .orElseGet(Set::of);
    }

    private CurrentAccountAccess toCurrentAccountAccess(AccountEntity accountEntity) {
        Set<String> permissionCodes = rolePermissionRepository
                .findActivePermissionCodesByRoleId(accountEntity.getRoleId());

        return new CurrentAccountAccess(
                accountEntity.getAccountId(),
                accountEntity.getKeycloakUserId(),
                accountEntity.getUsername(),
                accountEntity.getEmail(),
                accountEntity.getRoleId(),
                accountEntity.getStatus(),
                permissionCodes
        );
    }
}
