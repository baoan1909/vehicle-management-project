package com.ban.vehicle_management.application.people.employee.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.InternalEmployeeApprovalPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class EmployeeAccessGuardTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private InternalEmployeeApprovalPortOut internalEmployeeApprovalPortOut;

    @InjectMocks
    private EmployeeAccessGuard employeeAccessGuard;

    @Test
    void shouldAllowParkingManagerToManageEmployeeTarget() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employee(employeeId);

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentParkingManager());
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(employeeId, "EMPLOYEE")));

        employeeAccessGuard.ensureCanManage(employee);
    }

    @Test
    void shouldRejectParkingManagerManagingParkingManagerTarget() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employee(employeeId);

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentParkingManager());
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(employeeId, "PARKING_MANAGER")));

        assertThrows(AccessDeniedException.class, () -> employeeAccessGuard.ensureCanManage(employee));
    }

    @Test
    void shouldFilterParkingManagerListToEmployeeTargetsOnly() {
        UUID employeeId = UUID.randomUUID();
        UUID managerEmployeeId = UUID.randomUUID();
        Employee employee = employee(employeeId);
        Employee managerEmployee = employee(managerEmployeeId);

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentParkingManager());
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(employeeId))
                .thenReturn(Optional.of(candidate(employeeId, "EMPLOYEE")));
        when(internalEmployeeApprovalPortOut.findCandidateByEmployeeId(managerEmployeeId))
                .thenReturn(Optional.of(candidate(managerEmployeeId, "PARKING_MANAGER")));

        List<Employee> results = employeeAccessGuard.filterReadableEmployees(List.of(employee, managerEmployee));

        assertEquals(1, results.size());
        assertEquals(employeeId, results.get(0).getEmployeeId());
    }

    @Test
    void shouldAllowNonParkingManagerToReadFullList() {
        Employee employee = employee(UUID.randomUUID());
        Employee managerEmployee = employee(UUID.randomUUID());

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentSystemAdmin());

        List<Employee> results = employeeAccessGuard.filterReadableEmployees(List.of(employee, managerEmployee));

        assertEquals(2, results.size());
    }

    private Employee employee(UUID employeeId) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setUserProfileId(UUID.randomUUID());
        employee.setEmployeeCode("EMP-001");
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    private InternalEmployeeApprovalCandidate candidate(UUID employeeId, String roleCode) {
        return new InternalEmployeeApprovalCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                employeeId,
                roleCode,
                AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE
        );
    }

    private CurrentAccountAccess currentParkingManager() {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "sub-manager",
                "parking.manager",
                "parking.manager@example.com",
                UUID.randomUUID(),
                "PARKING_MANAGER",
                AccountStatus.ACTIVE,
                EmployeeStatus.ACTIVE,
                Set.of("EMPLOYEE_READ_ALL", "EMPLOYEE_UPDATE_ALL", "EMPLOYEE_DELETE_ALL")
        );
    }

    private CurrentAccountAccess currentSystemAdmin() {
        return new CurrentAccountAccess(
                UUID.randomUUID(),
                "sub-system-admin",
                "system.admin",
                "system.admin@example.com",
                UUID.randomUUID(),
                "SYSTEM_ADMIN",
                AccountStatus.ACTIVE,
                null,
                Set.of("EMPLOYEE_READ_ALL")
        );
    }
}
