package com.ban.vehicle_management.entrypoint.dto.people.employee.request;

import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.time.LocalDate;

public record UpdateEmployeeRequest(
        String employeeCode,
        String jobTitle,
        LocalDate hiredAt,
        EmployeeStatus status
) {
}

