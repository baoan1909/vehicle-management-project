package com.ban.vehicle_management.application.iam.role.port.in;

import com.ban.vehicle_management.domain.iam.role.model.Role;

import java.util.List;
import java.util.UUID;

public interface RolePortIn {

    Role createRole(Role role);
    Role updateRole(UUID roleId, Role role);
    Role getRoleById(UUID roleId);

    List<Role> getRoles(Boolean isActive, Boolean isSystem, String keyword);

    void deleteRole(UUID roleId);
}
