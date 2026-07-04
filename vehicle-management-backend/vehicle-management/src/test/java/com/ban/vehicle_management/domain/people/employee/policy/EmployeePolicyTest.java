package com.ban.vehicle_management.domain.people.employee.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmployeePolicyTest {

    private final EmployeePolicy employeePolicy = new EmployeePolicy();

    @Test
    void shouldInitializeEmployeeWithDefaults() {
        Employee employee = new Employee();
        employee.setUserProfileId(UUID.randomUUID());
        employee.setEmployeeCode(" EMP-001 ");
        employee.setJobTitle(" Cashier ");

        employeePolicy.initialize(employee);

        assertEquals("EMP-001", employee.getEmployeeCode());
        assertEquals("Cashier", employee.getJobTitle());
        assertEquals(EmployeeStatus.INACTIVE, employee.getStatus());
    }

    @Test
    void shouldRejectFutureHireDate() {
        Employee employee = new Employee();
        employee.setUserProfileId(UUID.randomUUID());
        employee.setEmployeeCode("EMP-001");
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setHiredAt(LocalDate.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> employeePolicy.validateState(employee));
    }

    @Test
    void shouldSetHireDateWhenActivatingEmployeeWithoutHireDate() {
        Employee employee = new Employee();
        employee.setUserProfileId(UUID.randomUUID());
        employee.setEmployeeCode("EMP-001");
        employee.setStatus(EmployeeStatus.INACTIVE);
        LocalDate hiredAt = LocalDate.of(2026, 6, 20);

        employeePolicy.activate(employee, hiredAt);

        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
        assertEquals(hiredAt, employee.getHiredAt());
    }

    @Test
    void shouldKeepExistingHireDateWhenActivatingEmployee() {
        Employee employee = new Employee();
        employee.setUserProfileId(UUID.randomUUID());
        employee.setEmployeeCode("EMP-001");
        employee.setStatus(EmployeeStatus.INACTIVE);
        employee.setHiredAt(LocalDate.of(2025, 1, 1));

        employeePolicy.activate(employee, LocalDate.of(2026, 6, 20));

        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
        assertEquals(LocalDate.of(2025, 1, 1), employee.getHiredAt());
    }

    @Test
    void shouldRejectEmployeeCodeWithUnsupportedCharacters() {
        Employee employee = new Employee();
        employee.setUserProfileId(UUID.randomUUID());
        employee.setEmployeeCode("emp<001>");

        assertThrows(BadRequestException.class, () -> employeePolicy.initialize(employee));
    }
}

