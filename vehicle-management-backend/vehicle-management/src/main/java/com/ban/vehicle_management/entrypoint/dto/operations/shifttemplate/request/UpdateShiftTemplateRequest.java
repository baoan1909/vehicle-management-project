package com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.request;

import java.time.LocalTime;

public record UpdateShiftTemplateRequest(
        String name,
        LocalTime startLocalTime,
        LocalTime endLocalTime
) {
}