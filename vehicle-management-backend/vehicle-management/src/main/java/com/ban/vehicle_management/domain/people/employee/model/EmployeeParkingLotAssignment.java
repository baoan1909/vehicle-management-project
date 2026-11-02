package com.ban.vehicle_management.domain.people.employee.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentSource;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeParkingLotAssignment extends AuditableDomainModel {
    private UUID assignmentId;
    private UUID organizationId;
    private UUID parkingLotId;
    private UUID employeeId;
    private EmployeeParkingLotAssignmentType assignmentType;
    private EmployeeParkingLotAssignmentStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private UUID assignedByAccountId;
    private EmployeeParkingLotAssignmentSource assignmentSource;
    private UUID endedByAccountId;
    private String endReason;
    private Long version;
}
