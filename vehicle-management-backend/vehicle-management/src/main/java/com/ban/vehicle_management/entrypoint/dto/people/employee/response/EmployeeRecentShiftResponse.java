package com.ban.vehicle_management.entrypoint.dto.people.employee.response;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRecentShiftResponse(
        UUID shiftId,
        UUID assignmentId,
        LocalDate shiftDate,
        ShiftType shiftType,
        String timeRange,
        String locationName,
        String roleInShift,
        ShiftAssignmentStatus status
) {
}
