package com.ban.vehicle_management.entrypoint.dto.people.employee.request;

import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;

public record EmployeeFilterRequest(
        EmployeeStatus status,
        String keyword
) {
}
