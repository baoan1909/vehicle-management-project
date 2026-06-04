package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.shared.enumeration.people.CustomerType;

public record UpdateCustomerAdminRequest(
        CustomerType customerType
) {
}
