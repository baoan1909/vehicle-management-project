package com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import java.time.LocalDate;

public record MyShiftAssignmentFilterRequest(
        LocalDate fromDate,
        LocalDate toDate,
        ShiftAssignmentStatus status
) {
}