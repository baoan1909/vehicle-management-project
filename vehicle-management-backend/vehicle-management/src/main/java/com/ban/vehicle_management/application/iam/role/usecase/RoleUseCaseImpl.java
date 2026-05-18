package com.ban.vehicle_management.application.iam.role.usecase;

import com.ban.vehicle_management.application.iam.role.port.in.RolePortIn;
import com.ban.vehicle_management.application.iam.role.port.out.RolePortOut;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.domain.iam.role.policy.RolePolicy;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

import java.util.List;
import java.util.UUID;

import com.ban.vehicle_management.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleUseCaseImpl implements RolePortIn {

    private final RolePortOut rolePortOut;
    private final RolePolicy rolePolicy = new RolePolicy();

    public RoleUseCaseImpl(RolePortOut rolePortOut) {
        this.rolePortOut = rolePortOut;
    }

    @Override
    @Transactional
    public Role createRole(Role role) {
        rolePolicy.initializeNewRole(role);

        if (rolePortOut.existsByCode(role.getCode())) {
            throw new ConflictException("Role code already exists");
        }

        role.setRoleId(UUID.randomUUID());
        return rolePortOut.save(role);
    }

    @Override
    @Transactional
    public Role updateRole(UUID roleId, Role role) {
        Role existingRole = rolePortOut.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found"));

        if (Boolean.TRUE.equals(existingRole.getIsSystem())) {
            throw new BadRequestException("System role cannot be updated");
        }

        existingRole.setCode(role.getCode());
        existingRole.setName(role.getName());
        existingRole.setDescription(role.getDescription());

        if (role.getIsActive() != null) {
            existingRole.setIsActive(role.getIsActive());
        }

        rolePolicy.validateMaintenance(existingRole);

        if (rolePortOut.existsByCodeAndRoleIdNot(existingRole.getCode(), roleId)) {
            throw new ConflictException("Role code already exists");
        }

        return rolePortOut.save(existingRole);
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleById(UUID roleId) {
        return rolePortOut.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getRoles(Boolean isActive, Boolean isSystem, String keyword) {
        return rolePortOut.findAll(isActive, isSystem, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public void deleteRole(UUID roleId) {
        Role existingRole = getRoleById(roleId);

        if (Boolean.FALSE.equals(existingRole.getIsActive())) {
            return;
        }

        if (rolePortOut.hasAccounts(roleId)) {
            throw new BadRequestException("Role is assigned to accounts and cannot be deactivated");
        }

        rolePolicy.deactivate(existingRole);
        rolePortOut.save(existingRole);
    }

    private String normalizeKeyword(String keyword) {
        return TextValidationUtils.normalizeNullableText(keyword, "keyword", 0);
    }

}
