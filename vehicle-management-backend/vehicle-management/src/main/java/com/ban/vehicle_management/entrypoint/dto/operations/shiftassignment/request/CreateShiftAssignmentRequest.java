package com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request;

import java.util.UUID;

public record CreateShiftAssignmentRequest(
        UUID employeeId,
        UUID gateId
) {
}