package com.ban.vehicle_management.application.people.employee.port.in;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.EmployeeStatus;
import java.util.List;
import java.util.UUID;

public interface EmployeePortIn {

    Employee createEmployee(Employee employee);

    Employee updateEmployee(UUID employeeId, Employee employee);

    Employee getEmployeeById(UUID employeeId);

    List<Employee> getEmployees(EmployeeStatus status, String keyword);

    void deleteEmployee(UUID employeeId);

    Employee activateEmployee(UUID employeeId);

    Employee inactivateEmployee(UUID employeeId);

    Employee suspendEmployee(UUID employeeId);
}
