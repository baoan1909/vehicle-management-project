package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionPrepaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "parking_session_prepayments", schema = "parking")
@Getter
@Setter
public class ParkingSessionPrepaymentEntity {
    @Id
    @Column(name = "prepayment_id", nullable = false)
    private UUID prepaymentId;
    @Column(name = "parking_session_id", nullable = false)
    private UUID parkingSessionId;
    @Column(name = "invoice_id", nullable = false, unique = true)
    private UUID invoiceId;
    @Column(name = "estimated_check_out_at", nullable = false)
    private Instant estimatedCheckOutAt;
    @Column(name = "projected_gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal projectedGrossAmount;
    @Column(name = "paid_before_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidBeforeAmount;
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingSessionPrepaymentStatus status;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "paid_at")
    private Instant paidAt;
    @Column(name = "policy_version", nullable = false)
    private String policyVersion;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
}
