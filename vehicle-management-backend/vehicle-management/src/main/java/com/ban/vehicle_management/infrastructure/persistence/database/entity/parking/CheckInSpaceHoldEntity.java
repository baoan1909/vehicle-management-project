package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "check_in_space_holds", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
public class CheckInSpaceHoldEntity extends AuditableEntity {
    @Id
    @Column(name = "hold_id", nullable = false)
    private UUID holdId;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;

    @Column(name = "parking_space_id", nullable = false)
    private UUID parkingSpaceId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Column(name = "lane_id", nullable = false)
    private UUID laneId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "customer_reservation_id")
    private UUID customerReservationId;

    @Column(name = "vehicle_type_id", nullable = false)
    private UUID vehicleTypeId;

    @Column(name = "license_plate_normalized", nullable = false, length = 20)
    private String licensePlateNormalized;

    @Column(name = "credential_hash", nullable = false, length = 64)
    private String credentialHash;

    @Column(name = "customer_type", nullable = false, length = 20)
    private String customerType;

    @Column(name = "operator_account_id", nullable = false)
    private UUID operatorAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CheckInSpaceHoldStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_session_id")
    private UUID consumedSessionId;

    @Column(name = "cancel_reason", length = 100)
    private String cancelReason;
}
