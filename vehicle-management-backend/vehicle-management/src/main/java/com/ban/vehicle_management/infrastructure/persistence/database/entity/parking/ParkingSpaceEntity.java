package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ManualOccupancyType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatusSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_spaces", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpaceEntity extends AuditableEntity {

    @Id
    @Column(name = "parking_space_id", nullable = false)
    private UUID parkingSpaceId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", referencedColumnName = "zone_id", insertable = false, updatable = false)
    private ZoneEntity zone;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "vehicle_type_id")
    private UUID vehicleTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", referencedColumnName = "vehicle_type_id", insertable = false, updatable = false)
    private VehicleTypeEntity vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingSpaceStatus status;

    @Column(name = "lifecycle_status", nullable = false)
    private String lifecycleStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_source", nullable = false)
    private ParkingSpaceStatusSource statusSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "manual_occupancy_type")
    private ManualOccupancyType manualOccupancyType;

    @Column(name = "status_reason")
    private String statusReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "x", precision = 10, scale = 2)
    private BigDecimal x;

    @Column(name = "y", precision = 10, scale = 2)
    private BigDecimal y;

    @Column(name = "width", precision = 10, scale = 2)
    private BigDecimal width;

    @Column(name = "height", precision = 10, scale = 2)
    private BigDecimal height;

    @Column(name = "rotation", precision = 6, scale = 2, nullable = false)
    private BigDecimal rotation;
}