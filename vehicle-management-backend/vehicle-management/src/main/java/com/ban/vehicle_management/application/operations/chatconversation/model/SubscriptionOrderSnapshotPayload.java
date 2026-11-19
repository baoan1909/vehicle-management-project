package com.ban.vehicle_management.application.operations.chatconversation.model;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionOrderSnapshotPayload(
        int schemaVersion,
        UUID subscriptionId,
        String orderCode,
        UUID parkingLotId,
        String parkingLotCode,
        String parkingLotName,
        String ticketTypeCode,
        String ticketTypeName,
        String licensePlate,
        String vehicleTypeName,
        String effectiveFrom,
        String effectiveTo,
        BigDecimal price,
        SubscriptionStatus status,
        String orderedAt,
        String sharedAt
) {
}
