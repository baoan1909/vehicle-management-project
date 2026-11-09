package com.ban.vehicle_management.domain.parking.customerreservation.model;

import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerReservation {
    private UUID reservationId;
    private UUID customerId;
    private UUID parkingLotId;
    private UUID zoneId;
    private UUID parkingSpaceId;
    private UUID vehicleTypeId;
    private String licensePlate;
    private String qrToken;
    private UUID invoiceId;
    private UUID parkingSessionId;
    private CustomerReservationStatus status;
    private Instant paymentHoldExpiresAt;
    private Instant paidAt;
    private Instant checkInDeadline;
    private Instant checkedInAt;
    private Instant closedAt;
    private BigDecimal prepaidAmount;
    private BigDecimal dayPrice;
    private BigDecimal nightPrice;
    private LocalTime dayStart;
    private LocalTime dayEnd;
    private String policyVersion;
    private Instant createdAt;
    private Instant updatedAt;
}
