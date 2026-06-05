package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.infrastructure.mapper.iam.PermissionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.PermissionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.iam.PermissionSpecifications;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PermissionPersistenceAdapter implements PermissionPortOut {

    private final PermissionRepository permissionRepository;
    private final PermissionPersistenceMapper permissionPersistenceMapper;

    public PermissionPersistenceAdapter(
            PermissionRepository permissionRepository,
            PermissionPersistenceMapper permissionPersistenceMapper
    ) {
        this.permissionRepository = permissionRepository;
        this.permissionPersistenceMapper = permissionPersistenceMapper;
    }

    @Override
    public List<Permission> findAll(String keyword) {
        return permissionRepository.findAll(PermissionSpecifications.withKeyword(keyword)).stream()
                .map(permissionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Permission> findByIds(Collection<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }

        return permissionRepository.findByPermissionIdIn(permissionIds).stream()
                .map(permissionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Permission> findByRoleId(UUID roleId) {
        return permissionRepository.findActiveByRoleId(roleId).stream()
                .map(permissionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Permission> findById(UUID permissionId) {
        return permissionRepository.findById(permissionId)
                .map(permissionPersistenceMapper::toDomain);
    }
}
