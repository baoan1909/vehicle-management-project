package com.ban.vehicle_management.entrypoint.dto.people.employee.request;

import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UpdateUserProfileRequest;

public record UpdateEmployeeAdminProfileRequest(
        UpdateUserProfileRequest userProfile,
        UpdateEmployeeRequest employee
) {
}
