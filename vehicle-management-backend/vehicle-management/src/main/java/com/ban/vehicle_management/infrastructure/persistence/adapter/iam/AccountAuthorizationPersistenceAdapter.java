package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RolePermissionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class AccountAuthorizationPersistenceAdapter implements AccountAuthorizationPortOut {

    private final AccountRepository accountRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;

    public AccountAuthorizationPersistenceAdapter(
            AccountRepository accountRepository,
            RolePermissionRepository rolePermissionRepository,
            RoleRepository roleRepository,
            EmployeeRepository employeeRepository
    ) {
        this.accountRepository = accountRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
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
        String roleCode = roleRepository.findById(accountEntity.getRoleId())
                .map(role -> role.getCode())
                .orElse(null);

        return new CurrentAccountAccess(
                accountEntity.getAccountId(),
                accountEntity.getKeycloakUserId(),
                accountEntity.getUsername(),
                accountEntity.getEmail(),
                accountEntity.getRoleId(),
                roleCode,
                accountEntity.getStatus(),
                resolveEmployeeStatus(accountEntity),
                permissionCodes
        );
    }

    private com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus resolveEmployeeStatus(AccountEntity accountEntity) {
        if (accountEntity.getUserProfileId() == null) {
            return null;
        }
        return employeeRepository.findByUserProfileId(accountEntity.getUserProfileId())
                .map(employee -> employee.getStatus())
                .orElse(null);
    }
}
