package com.ban.vehicle_management.application.parking.parkingsession.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.shift.port.out.ShiftPortOut;
import com.ban.vehicle_management.application.operations.shiftassignment.port.out.ShiftAssignmentPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.domain.operations.shiftassignment.model.ShiftAssignment;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class EmployeeParkingLotAccessGuardTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private EmployeePortOut employeePortOut;
    @Mock private ShiftAssignmentPortOut shiftAssignmentPortOut;
    @Mock private ShiftPortOut shiftPortOut;
    @InjectMocks private EmployeeParkingLotAccessGuard guard;

    @Test
    void activeShiftAllowsOnlyItsParkingLot() {
        UUID accountId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setShiftId(shiftId);
        Shift shift = new Shift();
        shift.setParkingLotId(lotId);
        shift.setStatus(ShiftStatus.OPEN);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(employeePortOut.findByAccountId(accountId)).thenReturn(Optional.of(employee));
        when(shiftAssignmentPortOut.findAll(null, null, employeeId, null,
                ShiftAssignmentStatus.ACTIVE, null, null, null)).thenReturn(List.of(assignment));
        when(shiftPortOut.findById(shiftId)).thenReturn(Optional.of(shift));

        assertDoesNotThrow(() -> guard.ensureCanOperate(lotId));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperate(UUID.randomUUID()));
    }

    @Test
    void noActiveShiftDeniesOperation() {
        UUID accountId = UUID.randomUUID();
        Employee employee = new Employee();
        employee.setEmployeeId(UUID.randomUUID());
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(employeePortOut.findByAccountId(accountId)).thenReturn(Optional.of(employee));
        when(shiftAssignmentPortOut.findAll(null, null, employee.getEmployeeId(), null,
                ShiftAssignmentStatus.ACTIVE, null, null, null)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperate(UUID.randomUUID()));
    }
}
