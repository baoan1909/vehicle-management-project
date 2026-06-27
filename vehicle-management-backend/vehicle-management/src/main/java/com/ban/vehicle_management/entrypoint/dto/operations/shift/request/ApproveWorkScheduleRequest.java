package com.ban.vehicle_management.entrypoint.dto.operations.shift.request;

import java.time.LocalDate;
import java.util.UUID;

public record ApproveWorkScheduleRequest(
        UUID parkingLotId,
        LocalDate weekStartDate
) {
}