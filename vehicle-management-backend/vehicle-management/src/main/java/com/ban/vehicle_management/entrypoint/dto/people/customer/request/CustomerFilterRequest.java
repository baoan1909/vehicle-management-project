package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerType;
import java.util.UUID;

public record CustomerFilterRequest(
        CustomerStatus status,
        CustomerApprovalStatus approvalStatus,
        CustomerType customerType,
        String keyword,
        UUID parkingLotId
) {
}

