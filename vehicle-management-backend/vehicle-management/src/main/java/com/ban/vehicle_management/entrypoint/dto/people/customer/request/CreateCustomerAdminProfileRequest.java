package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.CreateUserProfileRequest;
import java.util.List;

public record CreateCustomerAdminProfileRequest(
        CreateUserProfileRequest userProfile,
        CreateCustomerAdminRequest customer,
        List<CreateCustomerAdminVehicleRequest> customerVehicles
) {
}
