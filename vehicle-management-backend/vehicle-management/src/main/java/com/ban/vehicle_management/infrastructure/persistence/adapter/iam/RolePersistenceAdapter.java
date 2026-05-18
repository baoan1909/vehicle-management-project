package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.infrastructure.mapper.iam.RolePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.RoleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RoleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.iam.RoleSpecifications;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RolePersistenceAdapter implements RolePortOut {
    private final RoleRepository roleRepository;
    private final RolePersistenceMapper rolePersistenceMapper;
    private final AccountRepository accountRepository;


    public RolePersistenceAdapter(
            RoleRepository roleRepository,
            RolePersistenceMapper rolePersistenceMapper,
            AccountRepository accountRepository) {
        this.roleRepository = roleRepository;
        this.rolePersistenceMapper = rolePersistenceMapper;
        this.accountRepository = accountRepository;
    }

    @Override
    public Role save(Role role) {
        RoleEntity savedRoleEntity = roleRepository.save(rolePersistenceMapper.toEntity(role));
        return rolePersistenceMapper.toDomain(savedRoleEntity);
    }

    @Override
    public boolean existsByCode(String code) {
        return roleRepository.existsByCode(code);
    }
    @Override
    public Optional<Role> findById(UUID roleId) {
        return roleRepository.findById(roleId)
                .map(rolePersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByCodeAndRoleIdNot(String code, UUID roleId) {
        return roleRepository.existsByCodeAndRoleIdNot(code, roleId);
    }

    @Override
    public List<Role> findAll(Boolean isActive, Boolean isSystem, String keyword) {
        return roleRepository.findAll(RoleSpecifications.withFilters(isActive, isSystem, keyword)).stream()
                .map(rolePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean hasAccounts(UUID roleId) {
        return accountRepository.existsByRoleId(roleId);
    }
}
