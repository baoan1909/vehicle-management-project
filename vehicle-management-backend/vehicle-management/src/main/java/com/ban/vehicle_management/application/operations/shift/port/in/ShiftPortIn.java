package com.ban.vehicle_management.application.operations.shift.port.in;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShiftPortIn {

    List<Shift> generateWeek(
            UUID parkingLotId,
            LocalDate weekStartDate
    );

    List<Shift> approveWeek(
            UUID parkingLotId,
            LocalDate weekStartDate
    );

    Shift getShiftById(UUID shiftId);

    List<Shift> getShifts(
            UUID parkingLotId,
            LocalDate fromDate,
            LocalDate toDate,
            ShiftType shiftType,
            ShiftStatus status,
            UUID employeeId,
            String keyword
    );

    Shift openShift(
            UUID shiftId,
            BigDecimal openingCash,
            String note
    );

    Shift closeShift(
            UUID shiftId,
            BigDecimal closingCash,
            String note
    );

    Shift cancelShift(
            UUID shiftId,
            String reason
    );
}