package com.ban.vehicle_management.application.people.employee.model.command;

import com.ban.vehicle_management.domain.people.employee.model.Employee;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;

public record UpdateEmployeeAdminProfileCommand(
        UserProfile userProfile,
        Employee employee
) {
}
