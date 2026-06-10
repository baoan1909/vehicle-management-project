package com.ban.vehicle_management.application.people.employee.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.application.people.employee.port.in.EmployeePortIn;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.employee.policy.EmployeePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeUseCaseImpl implements EmployeePortIn {

    private static final String EMPLOYEE_READ_ALL = "EMPLOYEE_READ_ALL";
    private static final String EMPLOYEE_UPDATE_ALL = "EMPLOYEE_UPDATE_ALL";
    private static final String EMPLOYEE_DELETE_ALL = "EMPLOYEE_DELETE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final EmployeePortOut employeePortOut;
    private final InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;
    private final EmployeePolicy employeePolicy = new EmployeePolicy();

    public EmployeeUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            EmployeePortOut employeePortOut,
            InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.employeePortOut = employeePortOut;
        this.internalEmployeeApprovalPortOut = internalEmployeeApprovalPortOut;
    }

    @Override
    @Transactional
    public Employee updateEmployee(UUID employeeId, Employee employee) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee existingEmployee = getEmployeeById(employeeId);

        existingEmployee.setEmployeeCode(employee.getEmployeeCode());
        existingEmployee.setJobTitle(employee.getJobTitle());
        existingEmployee.setHiredAt(employee.getHiredAt());
        if (employee.getStatus() != null && !Objects.equals(employee.getStatus(), existingEmployee.getStatus())) {
            throw new BadRequestException("Use activate, inactivate, or suspend endpoints to change employee status");
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
        currentAccountPortIn.requirePermission(EMPLOYEE_READ_ALL);
        return employeePortOut.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getEmployees(EmployeeStatus status, String keyword) {
        currentAccountPortIn.requirePermission(EMPLOYEE_READ_ALL);
        return employeePortOut.findAll(status, keyword);
    }

    @Override
    @Transactional
    public void deleteEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_DELETE_ALL);
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
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        ensureInternalOnboardingApprovalSatisfied(employeeId);
        employeePolicy.activate(employee);
        return employeePortOut.save(employee);
    }

    @Override
    @Transactional
    public Employee inactivateEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        employeePolicy.inactivate(employee);
        return employeePortOut.save(employee);
    }

    @Override
    @Transactional
    public Employee suspendEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_UPDATE_ALL);
        Employee employee = getEmployeeById(employeeId);
        employeePolicy.suspend(employee);
        return employeePortOut.save(employee);
    }

    private void ensureInternalOnboardingApprovalSatisfied(UUID employeeId) {
        internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId)
                .filter(candidate -> requiresApproval(candidate.roleCode()))
                .ifPresent(candidate -> {
                    ApprovalRequest latestApprovalRequest = internalEmployeeApprovalPortOut
                            .findLatestInternalEmployeeApprovalRequest(employeeId)
                            .orElseThrow(() -> new ConflictException(
                                    "Internal employee activation requires an approved onboarding request"
                            ));
                    if (latestApprovalRequest.getStatus() != ApprovalRequestStatus.APPROVED) {
                        throw new ConflictException(
                                "Use the internal employee approval request flow before activating this employee"
                        );
                    }
                });
    }

    private boolean requiresApproval(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return false;
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode).requiresEmployeeRecord();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
