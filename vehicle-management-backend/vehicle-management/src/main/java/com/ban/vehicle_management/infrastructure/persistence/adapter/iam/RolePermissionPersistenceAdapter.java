package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.rolepermission.port.out.RolePermissionPortOut;
import com.ban.vehicle_management.domain.iam.role.model.RolePermission;
import com.ban.vehicle_management.infrastructure.mapper.iam.RolePermissionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.RolePermissionRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RolePermissionPersistenceAdapter implements RolePermissionPortOut {

    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionPersistenceMapper rolePermissionPersistenceMapper;

    public RolePermissionPersistenceAdapter(
            RolePermissionRepository rolePermissionRepository,
            RolePermissionPersistenceMapper rolePermissionPersistenceMapper
    ) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.rolePermissionPersistenceMapper = rolePermissionPersistenceMapper;
    }

    @Override
    public List<RolePermission> findByRoleId(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(rolePermissionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RolePermission> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId) {
        return rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId)
                .map(rolePermissionPersistenceMapper::toDomain);
    }

    @Override
    public List<RolePermission> saveAll(Collection<RolePermission> rolePermissions) {
        return rolePermissionRepository.saveAll(
                        rolePermissions.stream()
                                .map(rolePermissionPersistenceMapper::toEntity)
                                .toList()
                ).stream()
                .map(rolePermissionPersistenceMapper::toDomain)
                .toList();
    }
}
