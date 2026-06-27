package com.ban.vehicle_management.entrypoint.dto.operations.shift.request;

import java.math.BigDecimal;

public record CloseShiftRequest(
        BigDecimal closingCash,
        String note
) {
}