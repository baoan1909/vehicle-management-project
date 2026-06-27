package com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.request;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalTime;
import java.util.UUID;

public record CreateShiftTemplateRequest(
        UUID parkingLotId,
        ShiftType shiftType,
        String name,
        LocalTime startLocalTime,
        LocalTime endLocalTime
) {
}