package com.ban.vehicle_management.application.people.employee.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.employee.authorization.EmployeeAccessGuard;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeActivityTimelineResult;
import com.ban.vehicle_management.application.people.employee.model.result.EmployeeRecentShiftResult;
import com.ban.vehicle_management.application.people.employee.port.in.EmployeeManagerReadPortIn;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeeManagerReadPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagerReadUseCaseImpl implements EmployeeManagerReadPortIn {

    private static final String EMPLOYEE_READ_ALL = "EMPLOYEE_READ_ALL";
    private static final int DEFAULT_RECENT_SHIFT_LIMIT = 3;
    private static final int DEFAULT_TIMELINE_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final CurrentAccountPortIn currentAccountPortIn;
    private final EmployeeAccessGuard employeeAccessGuard;
    private final EmployeePortOut employeePortOut;
    private final EmployeeManagerReadPortOut employeeManagerReadPortOut;

    public EmployeeManagerReadUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            EmployeeAccessGuard employeeAccessGuard,
            EmployeePortOut employeePortOut,
            EmployeeManagerReadPortOut employeeManagerReadPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.employeeAccessGuard = employeeAccessGuard;
        this.employeePortOut = employeePortOut;
        this.employeeManagerReadPortOut = employeeManagerReadPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeRecentShiftResult> getRecentShifts(
            UUID employeeId,
            Integer limit
    ) {
        ensureCanReadEmployee(employeeId);
        return employeeManagerReadPortOut.findRecentShifts(
                employeeId,
                normalizeLimit(limit, DEFAULT_RECENT_SHIFT_LIMIT)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeActivityTimelineResult> getActivityTimeline(
            UUID employeeId,
            Integer limit
    ) {
        ensureCanReadEmployee(employeeId);
        return employeeManagerReadPortOut.findActivityTimeline(
                employeeId,
                normalizeLimit(limit, DEFAULT_TIMELINE_LIMIT)
        );
    }

    private void ensureCanReadEmployee(UUID employeeId) {
        currentAccountPortIn.requirePermission(EMPLOYEE_READ_ALL);
        Employee employee = employeePortOut.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));
        employeeAccessGuard.ensureCanRead(employee);
    }

    private int normalizeLimit(Integer limit, int defaultLimit) {
        int resolvedLimit = limit == null ? defaultLimit : limit;
        if (resolvedLimit < 1 || resolvedLimit > MAX_LIMIT) {
            throw new BadRequestException("limit must be between 1 and " + MAX_LIMIT);
        }
        return resolvedLimit;
    }
}
