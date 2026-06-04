package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import java.time.Instant;
import java.util.UUID;

public record ApproveCustomerRequest(
        Instant approvedAt
) {
}
