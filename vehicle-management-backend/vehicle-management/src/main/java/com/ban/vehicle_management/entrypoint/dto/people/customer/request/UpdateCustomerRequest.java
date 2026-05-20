package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.shared.enumeration.CustomerType;

public record UpdateCustomerRequest(
        String customerCode,
        CustomerType customerType
) {
}
