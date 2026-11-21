package com.ban.vehicle_management.domain.parking.parkingsessionprepayment.model;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionPrepaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSessionPrepayment {
    private UUID prepaymentId;
    private UUID parkingSessionId;
    private UUID invoiceId;
    private Instant estimatedCheckOutAt;
    private BigDecimal projectedGrossAmount;
    private BigDecimal paidBeforeAmount;
    private BigDecimal amount;
    private ParkingSessionPrepaymentStatus status;
    private Instant expiresAt;
    private Instant paidAt;
    private String policyVersion;
    private Instant createdAt;
    private Instant updatedAt;
}
