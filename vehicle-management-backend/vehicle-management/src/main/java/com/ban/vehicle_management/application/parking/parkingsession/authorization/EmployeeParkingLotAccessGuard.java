package com.ban.vehicle_management.application.parking.parkingsession.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.shift.port.out.ShiftPortOut;
import com.ban.vehicle_management.application.operations.shiftassignment.port.out.ShiftAssignmentPortOut;
import com.ban.vehicle_management.application.people.employee.port.out.EmployeePortOut;
import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** An Employee can operate only lots where their own shift is currently open. */
@Component
public class EmployeeParkingLotAccessGuard {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final EmployeePortOut employeePortOut;
    private final ShiftAssignmentPortOut shiftAssignmentPortOut;
    private final ShiftPortOut shiftPortOut;

    public EmployeeParkingLotAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            EmployeePortOut employeePortOut,
            ShiftAssignmentPortOut shiftAssignmentPortOut,
            ShiftPortOut shiftPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.employeePortOut = employeePortOut;
        this.shiftAssignmentPortOut = shiftAssignmentPortOut;
        this.shiftPortOut = shiftPortOut;
    }

    public Set<UUID> activeParkingLotIds() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Employee employee = employeePortOut.findByAccountId(accountId)
                .orElseThrow(() -> new AccessDeniedException("Employee profile not found"));
        return shiftAssignmentPortOut.findAll(
                        null, null, employee.getEmployeeId(), null,
                        ShiftAssignmentStatus.ACTIVE, null, null, null
                ).stream()
                .map(assignment -> shiftPortOut.findById(assignment.getShiftId()).orElse(null))
                .filter(shift -> shift != null && ShiftStatus.OPEN.equals(shift.getStatus()))
                .map(Shift::getParkingLotId)
                .collect(Collectors.toSet());
    }

    public void ensureCanOperate(UUID parkingLotId) {
        if (parkingLotId == null || !activeParkingLotIds().contains(parkingLotId)) {
            throw new AccessDeniedException("Employee is not on an active shift at this parking lot");
        }
    }
}
