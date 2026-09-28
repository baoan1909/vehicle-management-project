package com.ban.vehicle_management.entrypoint.dto.people.employee.request;

import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.UUID;

public record EmployeeFilterRequest(
        EmployeeStatus status,
        String keyword,
        UUID parkingLotId
) {
}
