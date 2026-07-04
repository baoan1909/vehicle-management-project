package com.ban.vehicle_management.application.people.employee.port.out;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeePortOut {

    Employee save(Employee employee);

    Optional<Employee> findById(UUID employeeId);

    List<Employee> findAll(EmployeeStatus status, String keyword);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCodeAndEmployeeIdNot(String employeeCode, UUID employeeId);

    boolean existsByUserProfileId(UUID userProfileId);

    boolean existsUserProfileById(UUID userProfileId);

    Optional<Employee> findByAccountId(UUID accountId);

    boolean hasAccountRole(UUID employeeId, String roleCode);
}

