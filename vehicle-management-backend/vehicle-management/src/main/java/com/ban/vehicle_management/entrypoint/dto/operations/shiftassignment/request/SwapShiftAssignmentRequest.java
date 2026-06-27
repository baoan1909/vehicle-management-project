package com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.request;

import java.util.UUID;

public record SwapShiftAssignmentRequest(
        UUID firstAssignmentId,
        UUID secondAssignmentId,
        String reason
) {
}