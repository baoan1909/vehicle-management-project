package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionQrVerificationStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionQrVerificationResponse {
    private SubscriptionQrVerificationStatus status;
    private String message;
    private String qrPayload;
    private UUID subscriptionId;
    private UUID parkingLotId;
    private UUID customerVehicleId;
    private String licensePlate;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
