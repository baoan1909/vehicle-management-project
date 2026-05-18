package com.ban.vehicle_management.domain.iam.role.policy;

import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class RolePolicy {
    public void initializeNewRole(Role role) {
        requireRole(role);
        role.setCode(TextValidationUtils.normalizeCode(role.getCode(), "code", 50));
        role.setName(TextValidationUtils.normalizeRequiredText(role.getName(), "name", 100));
        role.setDescription(TextValidationUtils.normalizeNullableText(role.getDescription(), "description", 0));
        role.setIsSystem(Boolean.FALSE);
        role.setIsActive(Boolean.TRUE);
    }

    private void requireRole(Role role) {
        if (role == null) {
            throw new BadRequestException("role must not be null");
        }
    }

    public void validateMaintenance(Role role) {
        requireRole(role);
        role.setCode(TextValidationUtils.normalizeCode(role.getCode(), "code", 50));
        role.setName(TextValidationUtils.normalizeRequiredText(role.getName(), "name", 100));
        role.setDescription(TextValidationUtils.normalizeNullableText(role.getDescription(), "description", 0));

        if (role.getIsActive() == null) {
            role.setIsActive(Boolean.TRUE);
        }
    }

    public void deactivate(Role role) {
        requireRole(role);

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BadRequestException("System role cannot be deactivated");
        }

        role.setIsActive(Boolean.FALSE);
    }

}
