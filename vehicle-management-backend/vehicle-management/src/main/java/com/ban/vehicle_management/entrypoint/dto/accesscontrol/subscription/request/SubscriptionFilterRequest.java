package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionFilterRequest(
        UUID customerId,
        UUID customerVehicleId,
        UUID cardId,
        UUID ticketTypeId,
        SubscriptionStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String keyword
) {
}