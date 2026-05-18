package com.ban.vehicle_management.application.iam.role.port.out;

import com.ban.vehicle_management.domain.iam.role.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePortOut {
    Role save(Role role);

    Optional<Role> findById(UUID roleId);

    List<Role> findAll(Boolean isActive, Boolean isSystem, String keyword);

    boolean existsByCode(String code);

    boolean existsByCodeAndRoleIdNot(String code, UUID roleId);

    boolean hasAccounts(UUID roleId);
}
