package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.shared.enumeration.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerType;

public record CustomerFilterRequest(
        CustomerStatus status,
        CustomerApprovalStatus approvalStatus,
        CustomerType customerType,
        String keyword
) {
}
