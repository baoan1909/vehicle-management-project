package com.ban.vehicle_management.application.people.customer.model.command;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;

public record UpdateCustomerAdminProfileCommand(
        UserProfile userProfile,
        Customer customer,
        UpdateCustomerAdminVehicleDiffCommand vehicles
) {
}
