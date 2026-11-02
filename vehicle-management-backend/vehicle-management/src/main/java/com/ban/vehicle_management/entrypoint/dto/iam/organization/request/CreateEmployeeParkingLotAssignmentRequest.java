package com.ban.vehicle_management.entrypoint.dto.iam.organization.request;

import com.ban.vehicle_management.shared.enumeration.people.EmployeeParkingLotAssignmentType;
import java.time.Instant;
import java.util.UUID;

public record CreateEmployeeParkingLotAssignmentRequest(
        UUID employeeId,
        EmployeeParkingLotAssignmentType assignmentType,
        Instant effectiveFrom,
        Instant effectiveTo
) {}
