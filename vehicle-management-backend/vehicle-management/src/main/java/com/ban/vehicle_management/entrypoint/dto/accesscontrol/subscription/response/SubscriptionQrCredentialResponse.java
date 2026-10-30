package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionQrCredentialResponse {
    private UUID subscriptionId;
    private UUID parkingLotId;
    private UUID customerVehicleId;
    private String qrPayload;
    private String qrCodeDataUrl;
    private Instant issuedAt;
    private boolean accessBlocked;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
