package com.ban.vehicle_management.entrypoint.dto.operations.shift.request;

import java.math.BigDecimal;

public record OpenShiftRequest(
        BigDecimal openingCash,
        String note
) {
}