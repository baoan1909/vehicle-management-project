package com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.UUID;

public record ShiftAssignmentFilterRequest(
        UUID parkingLotId,
        UUID shiftId,
        UUID employeeId,
        UUID gateId,
        ShiftAssignmentStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        ShiftType shiftType
) {
}