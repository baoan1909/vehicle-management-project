package com.ban.vehicle_management.application.accesscontrol.subscription.model.result;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionQrVerificationStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionQrVerification(
        SubscriptionQrVerificationStatus status,
        String message,
        String qrPayload,
        UUID subscriptionId,
        UUID parkingLotId,
        UUID customerVehicleId,
        String licensePlate,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}
