package com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.request;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.util.UUID;

public record ShiftTemplateFilterRequest(
        UUID parkingLotId,
        ShiftType shiftType,
        ShiftTemplateStatus status,
        String keyword
) {
}