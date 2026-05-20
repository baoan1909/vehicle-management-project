package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import java.util.UUID;

public record CreateCustomerRequest(
        UUID userProfileId,
        String customerCode,
        CustomerType customerType
) {
}

