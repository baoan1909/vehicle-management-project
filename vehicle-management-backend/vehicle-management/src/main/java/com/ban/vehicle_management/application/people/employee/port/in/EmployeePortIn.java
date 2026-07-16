package com.ban.vehicle_management.application.people.employee.port.in;

import com.ban.vehicle_management.application.people.employee.model.command.UpdateEmployeeAdminProfileCommand;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface EmployeePortIn {

    Employee updateEmployee(UUID employeeId, Employee employee);

    Employee updateEmployeeAdminProfile(UUID employeeId, UpdateEmployeeAdminProfileCommand command);

    Employee uploadEmployeeAvatar(UUID employeeId, MultipartFile file);

    Employee deleteEmployeeAvatar(UUID employeeId);

    Employee getEmployeeById(UUID employeeId);

    List<Employee> getEmployees(EmployeeStatus status, String keyword);

    void deleteEmployee(UUID employeeId);

    Employee activateEmployee(UUID employeeId);

    Employee inactivateEmployee(UUID employeeId);

    Employee suspendEmployee(UUID employeeId);
}

