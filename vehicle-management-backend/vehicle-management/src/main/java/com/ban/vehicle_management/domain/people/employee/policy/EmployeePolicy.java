package com.ban.vehicle_management.domain.people.employee.policy;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.LocalDate;

public class EmployeePolicy {

    public void initialize(Employee employee) {
        requireEmployee(employee);
        requireField(employee.getUserProfileId(), "userProfileId");
        employee.setEmployeeCode(normalizeEmployeeCode(employee.getEmployeeCode()));
        employee.setJobTitle(TextValidationUtils.normalizeNullableText(employee.getJobTitle(), "jobTitle", 100));
        if (employee.getStatus() == null) {
            employee.setStatus(EmployeeStatus.INACTIVE);
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
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE employee can be suspended");
        }
        employee.setStatus(EmployeeStatus.SUSPENDED);
        validateState(employee);
    }

    public void validateState(Employee employee) {
        requireEmployee(employee);
        requireField(employee.getUserProfileId(), "userProfileId");
        employee.setEmployeeCode(normalizeEmployeeCode(employee.getEmployeeCode()));
        employee.setJobTitle(TextValidationUtils.normalizeNullableText(employee.getJobTitle(), "jobTitle", 100));
        requireField(employee.getStatus(), "status");

        if (employee.getHiredAt() != null && employee.getHiredAt().isAfter(LocalDate.now())) {
            throw new BadRequestException("hiredAt must not be in the future");
        }
    }

    private void requireEmployee(Employee employee) {
        requireField(employee, "employee");
    }

    private String normalizeEmployeeCode(String employeeCode) {
        if (employeeCode == null || employeeCode.isBlank()) {
            return null;
        }
        return TextValidationUtils.normalizeCode(employeeCode, "employeeCode", 50);
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
