package com.ban.vehicle_management.application.accesscontrol.subscription.model.command;

import java.util.UUID;

public record VerifySubscriptionQrCommand(
        String qrPayload,
        UUID parkingLotId,
        String licensePlate
) {
}
