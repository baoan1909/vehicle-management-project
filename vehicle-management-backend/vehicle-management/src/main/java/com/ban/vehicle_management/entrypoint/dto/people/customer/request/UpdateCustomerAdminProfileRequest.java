package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UpdateUserProfileRequest;

public record UpdateCustomerAdminProfileRequest(
        UpdateUserProfileRequest userProfile,
        UpdateCustomerAdminRequest customer
) {
}
