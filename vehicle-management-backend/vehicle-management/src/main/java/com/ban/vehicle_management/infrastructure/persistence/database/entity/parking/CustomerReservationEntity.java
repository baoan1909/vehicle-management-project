package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_reservations", schema = "parking")
@Getter
@Setter
public class CustomerReservationEntity {
    @Id @Column(name = "reservation_id") private UUID reservationId;
    @Column(name = "customer_id") private UUID customerId;
    @Column(name = "parking_lot_id", nullable = false) private UUID parkingLotId;
    @Column(name = "zone_id", nullable = false) private UUID zoneId;
    @Column(name = "parking_space_id") private UUID parkingSpaceId;
    @Column(name = "vehicle_type_id", nullable = false) private UUID vehicleTypeId;
    @Column(name = "license_plate", nullable = false) private String licensePlate;
    @Column(name = "qr_token", nullable = false) private String qrToken;
    @Column(name = "invoice_id", nullable = false) private UUID invoiceId;
    @Column(name = "parking_session_id") private UUID parkingSessionId;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private CustomerReservationStatus status;
    @Column(name = "payment_hold_expires_at", nullable = false) private Instant paymentHoldExpiresAt;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "check_in_deadline") private Instant checkInDeadline;
    @Column(name = "checked_in_at") private Instant checkedInAt;
    @Column(name = "closed_at") private Instant closedAt;
    @Column(name = "prepaid_amount", nullable = false) private BigDecimal prepaidAmount;
    @Column(name = "day_price", nullable = false) private BigDecimal dayPrice;
    @Column(name = "night_price", nullable = false) private BigDecimal nightPrice;
    @Column(name = "day_start", nullable = false) private LocalTime dayStart;
    @Column(name = "day_end", nullable = false) private LocalTime dayEnd;
    @Column(name = "policy_version", nullable = false) private String policyVersion;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
