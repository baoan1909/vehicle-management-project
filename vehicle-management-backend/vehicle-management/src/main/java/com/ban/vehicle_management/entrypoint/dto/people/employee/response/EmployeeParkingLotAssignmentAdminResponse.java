package com.ban.vehicle_management.entrypoint.dto.people.employee.response;

import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentSource;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentType;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeParkingLotAssignmentAdminResponse {
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
