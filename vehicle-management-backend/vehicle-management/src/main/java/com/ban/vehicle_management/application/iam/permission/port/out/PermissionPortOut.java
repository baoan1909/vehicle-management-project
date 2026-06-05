package com.ban.vehicle_management.application.iam.permission.port.out;

import com.ban.vehicle_management.domain.iam.permission.model.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionPortOut {

    List<Permission> findAll(String keyword);

    List<Permission> findByIds(Collection<UUID> permissionIds);

    List<Permission> findByRoleId(UUID roleId);

    Optional<Permission> findById(UUID permissionId);
}
