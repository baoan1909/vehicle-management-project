package com.ban.vehicle_management.entrypoint.dto.operations.shift.request;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalDate;
import java.util.UUID;

public record ShiftFilterRequest(
        UUID parkingLotId,
        LocalDate fromDate,
        LocalDate toDate,
        ShiftType shiftType,
        ShiftStatus status,
        UUID employeeId,
        String keyword
) {
}