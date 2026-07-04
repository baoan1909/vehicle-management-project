package com.ban.vehicle_management.domain.operations.shiftassignment.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftAssignment extends AuditableDomainModel {

    private UUID shiftAssignmentId;
    private UUID shiftId;
    private UUID employeeId;
    private UUID gateId;
    private ShiftAssignmentStatus status;
}