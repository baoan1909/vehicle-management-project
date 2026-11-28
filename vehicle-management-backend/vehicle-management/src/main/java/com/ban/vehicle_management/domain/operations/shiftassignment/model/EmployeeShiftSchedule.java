package com.ban.vehicle_management.domain.operations.shiftassignment.model;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;

public record EmployeeShiftSchedule(ShiftAssignment assignment, Shift shift, String parkingLotName) {
}
