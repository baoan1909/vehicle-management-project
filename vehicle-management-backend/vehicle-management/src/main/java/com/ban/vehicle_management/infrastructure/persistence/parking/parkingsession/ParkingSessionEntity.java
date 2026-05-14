package com.ban.vehicle_management.infrastructure.persistence.parking.parkingsession;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.ParkingSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_sessions", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSessionEntity extends AuditableEntity {

    @Id
    @Column(name = "parking_session_id", nullable = false)
    private UUID parkingSessionId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_vehicle_id")
    private UUID customerVehicleId;

    @Column(name = "vehicle_type_id", nullable = false)
    private UUID vehicleTypeId;

    @Column(name = "parking_space_id")
    private UUID parkingSpaceId;

    @Column(name = "license_plate_in", nullable = false)
    private String licensePlateIn;

    @Column(name = "license_plate_out")
    private String licensePlateOut;

    @Column(name = "check_in_time", nullable = false)
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingSessionStatus status;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "price_rule_id")
    private UUID priceRuleId;

}
