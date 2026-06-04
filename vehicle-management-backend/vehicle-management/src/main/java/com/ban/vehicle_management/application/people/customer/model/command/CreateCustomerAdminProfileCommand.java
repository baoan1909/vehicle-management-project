package com.ban.vehicle_management.application.people.customer.model.command;

import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import java.util.List;

public record CreateCustomerAdminProfileCommand(
        UserProfile userProfile,
        Customer customer,
        List<CustomerVehicle> customerVehicles
) {
}
