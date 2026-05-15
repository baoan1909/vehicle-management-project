package com.ban.vehicle_management.domain.people.employee.policy;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;

public class EmployeePolicy {

    public void initialize(Employee employee) {
        requireEmployee(employee);
        requireField(employee.getUserProfileId(), "userProfileId");
        employee.setEmployeeCode(normalizeRequired(employee.getEmployeeCode(), "employeeCode"));
        employee.setJobTitle(normalizeNullable(employee.getJobTitle()));
        if (employee.getStatus() == null) {
            employee.setStatus(EmployeeStatus.ACTIVE);
        }
        validateState(employee);
    }

    public void activate(Employee employee) {
        requireEmployee(employee);
        employee.setStatus(EmployeeStatus.ACTIVE);
        validateState(employee);
    }

    public void inactivate(Employee employee) {
        requireEmployee(employee);
        employee.setStatus(EmployeeStatus.INACTIVE);
        validateState(employee);
    }

    public void suspend(Employee employee) {
        requireEmployee(employee);
        employee.setStatus(EmployeeStatus.SUSPENDED);
        validateState(employee);
    }

    public void validateState(Employee employee) {
        requireEmployee(employee);
        requireField(employee.getUserProfileId(), "userProfileId");
        employee.setEmployeeCode(normalizeRequired(employee.getEmployeeCode(), "employeeCode"));
        employee.setJobTitle(normalizeNullable(employee.getJobTitle()));
        requireField(employee.getStatus(), "status");

        if (employee.getHiredAt() != null && employee.getHiredAt().isAfter(LocalDate.now())) {
            throw new BadRequestException("hiredAt must not be in the future");
        }
    }

    private void requireEmployee(Employee employee) {
        requireField(employee, "employee");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
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
}

