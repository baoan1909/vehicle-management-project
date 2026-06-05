package com.ban.vehicle_management.application.iam.rolepermission.port.out;

import com.ban.vehicle_management.domain.iam.role.model.RolePermission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionPortOut {

    List<RolePermission> findByRoleId(UUID roleId);

    Optional<RolePermission> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    List<RolePermission> saveAll(Collection<RolePermission> rolePermissions);
}
