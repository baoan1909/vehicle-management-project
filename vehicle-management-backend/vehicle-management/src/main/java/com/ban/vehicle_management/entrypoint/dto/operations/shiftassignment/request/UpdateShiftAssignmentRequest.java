package com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request;

import java.util.UUID;

public record UpdateShiftAssignmentRequest(
        UUID employeeId,
        UUID gateId
) {
}