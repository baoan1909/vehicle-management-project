package com.ban.vehicle_management.domain.iam.role.policy;

import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.shared.exception.BadRequestException;

public class RolePolicy {
    public void initializeNewRole(Role role) {
        requireRole(role);
        role.setCode(normalizeRequired(role.getCode(), "code").toUpperCase());
        validateCodeFormat(role.getCode());
        role.setName(normalizeRequired(role.getName(), "name"));
        role.setDescription(normalizeNullable(role.getDescription()));
        role.setIsSystem(Boolean.FALSE);
        role.setIsActive(Boolean.TRUE);
    }

    private void requireRole(Role role) {
        if (role == null) {
            throw new BadRequestException("role must not be null");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private void validateCodeFormat(String code) {
        if (!code.matches("^[A-Z0-9_]+$")) {
            throw new BadRequestException("code must contain only uppercase letters, numbers, and underscore");
        }
    }

    public void validateMaintenance(Role role) {
        requireRole(role);
        role.setCode(normalizeRequired(role.getCode(), "code").toUpperCase());
        validateCodeFormat(role.getCode());
        role.setName(normalizeRequired(role.getName(), "name"));
        role.setDescription(normalizeNullable(role.getDescription()));

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
