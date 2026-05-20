package com.ban.vehicle_management.application.people.employee.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeUseCaseImplTest {

    @Mock
    private EmployeePortOut employeePortOut;

    @InjectMocks
    private EmployeeUseCaseImpl employeeUseCase;

    @Test
    void shouldCreateEmployeeWithDefaultActiveStatus() {
        Employee requestEmployee = new Employee();
        requestEmployee.setUserProfileId(UUID.randomUUID());
        requestEmployee.setEmployeeCode(" emp-001 ");
        requestEmployee.setJobTitle(" Cashier ");

        when(employeePortOut.existsUserProfileById(requestEmployee.getUserProfileId())).thenReturn(true);
        when(employeePortOut.existsByEmployeeCode("EMP-001")).thenReturn(false);
        when(employeePortOut.existsByUserProfileId(requestEmployee.getUserProfileId())).thenReturn(false);
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee createdEmployee = employeeUseCase.createEmployee(requestEmployee);

        assertEquals("EMP-001", createdEmployee.getEmployeeCode());
        assertEquals("Cashier", createdEmployee.getJobTitle());
        assertEquals(EmployeeStatus.ACTIVE, createdEmployee.getStatus());
    }

    @Test
    void shouldRejectCreateWhenUserProfileDoesNotExist() {
        Employee requestEmployee = new Employee();
        requestEmployee.setUserProfileId(UUID.randomUUID());
        requestEmployee.setEmployeeCode("EMP-001");

        when(employeePortOut.existsUserProfileById(requestEmployee.getUserProfileId())).thenReturn(false);

        assertThrows(NotFoundException.class, () -> employeeUseCase.createEmployee(requestEmployee));
        verify(employeePortOut, never()).save(any(Employee.class));
    }

    @Test
    void shouldRejectDuplicateEmployeeCodeOnCreate() {
        Employee requestEmployee = new Employee();
        requestEmployee.setUserProfileId(UUID.randomUUID());
        requestEmployee.setEmployeeCode("EMP-001");

        when(employeePortOut.existsUserProfileById(requestEmployee.getUserProfileId())).thenReturn(true);
        when(employeePortOut.existsByEmployeeCode("EMP-001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> employeeUseCase.createEmployee(requestEmployee));
    }

    @Test
    void shouldRejectLinkedUserProfileOnCreate() {
        Employee requestEmployee = new Employee();
        requestEmployee.setUserProfileId(UUID.randomUUID());
        requestEmployee.setEmployeeCode("EMP-001");

        when(employeePortOut.existsUserProfileById(requestEmployee.getUserProfileId())).thenReturn(true);
        when(employeePortOut.existsByEmployeeCode("EMP-001")).thenReturn(false);
        when(employeePortOut.existsByUserProfileId(requestEmployee.getUserProfileId())).thenReturn(true);

        assertThrows(ConflictException.class, () -> employeeUseCase.createEmployee(requestEmployee));
    }

    @Test
    void shouldUpdateEmployeeMetadata() {
        UUID employeeId = UUID.randomUUID();
        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeId(employeeId);
        existingEmployee.setUserProfileId(UUID.randomUUID());
        existingEmployee.setEmployeeCode("EMP-001");
        existingEmployee.setJobTitle("Cashier");
        existingEmployee.setStatus(EmployeeStatus.ACTIVE);

        Employee requestEmployee = new Employee();
        requestEmployee.setEmployeeCode("EMP-002");
        requestEmployee.setJobTitle("Supervisor");
        requestEmployee.setHiredAt(LocalDate.of(2025, 1, 1));
        requestEmployee.setStatus(EmployeeStatus.SUSPENDED);

        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeePortOut.existsByEmployeeCodeAndEmployeeIdNot("EMP-002", employeeId)).thenReturn(false);
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updatedEmployee = employeeUseCase.updateEmployee(employeeId, requestEmployee);

        assertEquals("EMP-002", updatedEmployee.getEmployeeCode());
        assertEquals("Supervisor", updatedEmployee.getJobTitle());
        assertEquals(LocalDate.of(2025, 1, 1), updatedEmployee.getHiredAt());
        assertEquals(EmployeeStatus.SUSPENDED, updatedEmployee.getStatus());
    }

    @Test
    void shouldReturnFilteredEmployees() {
        when(employeePortOut.findAll(EmployeeStatus.ACTIVE, "nguyen"))
                .thenReturn(List.of(new Employee(), new Employee()));

        List<Employee> employees = employeeUseCase.getEmployees(EmployeeStatus.ACTIVE, "nguyen");

        assertEquals(2, employees.size());
        verify(employeePortOut).findAll(EmployeeStatus.ACTIVE, "nguyen");
    }

    @Test
    void shouldSoftDeleteEmployeeBySettingInactiveStatus() {
        UUID employeeId = UUID.randomUUID();
        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeId(employeeId);
        existingEmployee.setUserProfileId(UUID.randomUUID());
        existingEmployee.setEmployeeCode("EMP-001");
        existingEmployee.setStatus(EmployeeStatus.ACTIVE);

        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(existingEmployee));
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        employeeUseCase.deleteEmployee(employeeId);

        assertEquals(EmployeeStatus.INACTIVE, existingEmployee.getStatus());
        verify(employeePortOut).save(existingEmployee);
    }

    @Test
    void shouldActivateEmployee() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = validEmployee(employeeId, EmployeeStatus.INACTIVE);

        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updatedEmployee = employeeUseCase.activateEmployee(employeeId);

        assertEquals(EmployeeStatus.ACTIVE, updatedEmployee.getStatus());
    }

    @Test
    void shouldSuspendEmployee() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = validEmployee(employeeId, EmployeeStatus.ACTIVE);

        when(employeePortOut.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeePortOut.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updatedEmployee = employeeUseCase.suspendEmployee(employeeId);

        assertEquals(EmployeeStatus.SUSPENDED, updatedEmployee.getStatus());
    }

    @Test
    void shouldThrowWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(employeePortOut.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> employeeUseCase.getEmployeeById(employeeId));
    }

    private Employee validEmployee(UUID employeeId, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setUserProfileId(UUID.randomUUID());
        employee.setEmployeeCode("EMP-001");
        employee.setJobTitle("Cashier");
        employee.setStatus(status);
        return employee;
    }
}
