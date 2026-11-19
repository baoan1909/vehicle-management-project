package com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ChatSubscriptionOrderUserResponse(
        UUID subscriptionId,
        UUID parkingLotId,
        String parkingLotCode,
        String parkingLotName,
        String ticketTypeCode,
        String ticketTypeName,
        String licensePlate,
        String vehicleTypeName,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal price,
        SubscriptionStatus status,
        Instant createdAt
) {
}
