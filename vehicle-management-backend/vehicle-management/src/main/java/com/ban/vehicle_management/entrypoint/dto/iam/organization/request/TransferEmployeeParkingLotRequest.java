package com.ban.vehicle_management.entrypoint.dto.iam.organization.request;

import java.time.Instant;
import java.util.UUID;

public record TransferEmployeeParkingLotRequest(
        UUID employeeId,
        UUID sourceParkingLotId,
        UUID targetParkingLotId,
        Instant effectiveAt,
        String reason
) {}
