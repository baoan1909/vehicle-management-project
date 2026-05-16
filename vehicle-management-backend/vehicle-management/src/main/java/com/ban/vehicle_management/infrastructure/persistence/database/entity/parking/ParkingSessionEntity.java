package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingEventEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSpaceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.shared.enumeration.ParkingSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", referencedColumnName = "card_id", insertable = false, updatable = false)
    private CardEntity card;

    @Column(name = "customer_id")
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", insertable = false, updatable = false)
    private CustomerEntity customer;

    @Column(name = "customer_vehicle_id")
    private UUID customerVehicleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_vehicle_id", referencedColumnName = "customer_vehicle_id", insertable = false, updatable = false)
    private CustomerVehicleEntity customerVehicle;

    @Column(name = "vehicle_type_id", nullable = false)
    private UUID vehicleTypeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id", referencedColumnName = "vehicle_type_id", insertable = false, updatable = false)
    private VehicleTypeEntity vehicleType;

    @Column(name = "parking_space_id")
    private UUID parkingSpaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_space_id", referencedColumnName = "parking_space_id", insertable = false, updatable = false)
    private ParkingSpaceEntity parkingSpace;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_rule_id", referencedColumnName = "price_rule_id", insertable = false, updatable = false)
    private PriceRuleEntity priceRule;

    @OneToMany(mappedBy = "parkingSession")
    private Set<ParkingEventEntity> parkingEvents = new HashSet<>();

    @OneToMany(mappedBy = "parkingSession")
    private Set<LostCardReportEntity> lostCardReports = new HashSet<>();

    @OneToMany(mappedBy = "parkingSession")
    private Set<InvoiceEntity> invoices = new HashSet<>();

}


