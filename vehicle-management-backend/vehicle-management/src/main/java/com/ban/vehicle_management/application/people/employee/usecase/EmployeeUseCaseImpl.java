package com.ban.vehicle_management.application.people.employee.usecase;

import com.ban.vehicle_management.application.people.employee.port.in.EmployeePortIn;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.employee.policy.EmployeePolicy;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeUseCaseImpl implements EmployeePortIn {

    private final EmployeePortOut employeePortOut;
    private final EmployeePolicy employeePolicy = new EmployeePolicy();

    public EmployeeUseCaseImpl(EmployeePortOut employeePortOut) {
        this.employeePortOut = employeePortOut;
    }

    @Override
    @Transactional
    public Employee createEmployee(Employee employee) {
        employeePolicy.initialize(employee);
        validateUserProfileExists(employee.getUserProfileId());

        if (employeePortOut.existsByEmployeeCode(employee.getEmployeeCode())) {
            throw new ConflictException("Employee code already exists");
        }
        if (employeePortOut.existsByUserProfileId(employee.getUserProfileId())) {
            throw new ConflictException("User profile is already linked to another employee");
        }

        employee.setEmployeeId(UUID.randomUUID());
        return employeePortOut.save(employee);
    }

    @Override
    @Transactional
    public Employee updateEmployee(UUID employeeId, Employee employee) {
        Employee existingEmployee = getEmployeeById(employeeId);

        existingEmployee.setEmployeeCode(employee.getEmployeeCode());
        existingEmployee.setJobTitle(employee.getJobTitle());
        existingEmployee.setHiredAt(employee.getHiredAt());
        if (employee.getStatus() != null) {
            existingEmployee.setStatus(employee.getStatus());
        }

        employeePolicy.validateState(existingEmployee);

        if (employeePortOut.existsByEmployeeCodeAndEmployeeIdNot(existingEmployee.getEmployeeCode(), employeeId)) {
            throw new ConflictException("Employee code already exists");
        }

        return employeePortOut.save(existingEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployeeById(UUID employeeId) {
        return employeePortOut.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getEmployees(EmployeeStatus status, String keyword) {
        return employeePortOut.findAll(status, keyword);
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID employeeId) {
        Employee existingEmployee = getEmployeeById(employeeId);
        if (existingEmployee.getStatus() == EmployeeStatus.INACTIVE) {
            return;
        }

        employeePolicy.inactivate(existingEmployee);
        employeePortOut.save(existingEmployee);
    }

    @Override
    @Transactional
    public Employee activateEmployee(UUID employeeId) {
        Employee employee = getEmployeeById(employeeId);
        employeePolicy.activate(employee);
        return employeePortOut.save(employee);
    }

    @Override
    @Transactional
    public Employee inactivateEmployee(UUID employeeId) {
        Employee employee = getEmployeeById(employeeId);
        employeePolicy.inactivate(employee);
        return employeePortOut.save(employee);
    }

    @Override
    @Transactional
    public Employee suspendEmployee(UUID employeeId) {
        Employee employee = getEmployeeById(employeeId);
        employeePolicy.suspend(employee);
        return employeePortOut.save(employee);
    }

    private void validateUserProfileExists(UUID userProfileId) {
        if (!employeePortOut.existsUserProfileById(userProfileId)) {
            throw new NotFoundException("User profile not found");
        }
    }
}

