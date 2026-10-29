package com.ban.vehicle_management.application.accesscontrol.subscription.model.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionQrCredential(
        UUID subscriptionId,
        UUID parkingLotId,
        UUID customerVehicleId,
        String qrPayload,
        String qrCodeDataUrl,
        Instant issuedAt,
        boolean accessBlocked,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
