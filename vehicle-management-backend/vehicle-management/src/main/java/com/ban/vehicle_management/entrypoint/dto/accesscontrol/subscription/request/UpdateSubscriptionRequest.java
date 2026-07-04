package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSubscriptionRequest(
        UUID customerVehicleId,
        UUID ticketTypeId,
        LocalDate requestedEffectiveFrom
) {
}