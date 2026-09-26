package com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.response;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShiftAssignmentAdminResponse {

    private UUID shiftAssignmentId;
    private UUID shiftId;
    private UUID employeeId;
    private UUID gateId;
    private ShiftAssignmentStatus status;
    private java.time.Instant createdAt;
    private UUID createdBy;
    private java.time.Instant updatedAt;
    private UUID updatedBy;
}
