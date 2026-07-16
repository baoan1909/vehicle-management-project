package com.ban.vehicle_management.application.people.employee.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.employee.authorization.EmployeeAccessGuard;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeeManagerReadPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeManagerReadUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private EmployeeAccessGuard employeeAccessGuard;

    @Mock
    private EmployeePortOut employeePortOut;

    @Mock
    private EmployeeManagerReadPortOut employeeManagerReadPortOut;

    @InjectMocks
    private EmployeeManagerReadUseCaseImpl useCase;

    @Test
    void shouldFetchRecentShiftsWithDefaultLimit() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employee(employeeId);
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeManagerReadPortOut.findRecentShifts(employeeId, 3)).thenReturn(List.of());

        useCase.getRecentShifts(employeeId, null);

        verify(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        verify(employeeAccessGuard).ensureCanRead(employee);
        verify(employeeManagerReadPortOut).findRecentShifts(employeeId, 3);
    }

    @Test
    void shouldFetchActivityTimelineWithProvidedLimit() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employee(employeeId);
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeManagerReadPortOut.findActivityTimeline(employeeId, 7)).thenReturn(List.of());

        useCase.getActivityTimeline(employeeId, 7);

        verify(currentAccountPortIn).requirePermission("EMPLOYEE_READ_ALL");
        verify(employeeAccessGuard).ensureCanRead(employee);
        verify(employeeManagerReadPortOut).findActivityTimeline(employeeId, 7);
    }

    @Test
    void shouldRejectInvalidLimit() {
        UUID employeeId = UUID.randomUUID();
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee(employeeId)));

        assertThrows(BadRequestException.class, () -> useCase.getRecentShifts(employeeId, 21));
    }

    @Test
    void shouldRejectMissingEmployee() {
        UUID employeeId = UUID.randomUUID();
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.getActivityTimeline(employeeId, 5));
    }

    private Employee employee(UUID employeeId) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeCode("EMP-001");
        return employee;
    }
}
