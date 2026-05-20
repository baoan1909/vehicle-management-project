package com.ban.vehicle_management.entrypoint.dto.people.employee.request;

import com.ban.vehicle_management.shared.enumeration.EmployeeStatus;
import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRequest(
        UUID userProfileId,
        String employeeCode,
        String jobTitle,
        LocalDate hiredAt,
        EmployeeStatus status
) {
}
