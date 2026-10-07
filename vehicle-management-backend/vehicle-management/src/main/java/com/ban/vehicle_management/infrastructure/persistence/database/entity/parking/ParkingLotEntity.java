package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.enumeration.parking.AddressInputScheme;
import com.ban.vehicle_management.shared.enumeration.parking.GeocodingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_lots", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotEntity extends AuditableEntity {

    @Id
    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_input_scheme")
    private AddressInputScheme addressInputScheme;

    @Column(name = "address_display", length = 500)
    private String addressDisplay;

    @Column(name = "current_ward_code", length = 20)
    private String currentWardCode;

    @Column(name = "legacy_ward_code", length = 20)
    private String legacyWardCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "geocoding_status", nullable = false)
    private GeocodingStatus geocodingStatus;

    @Column(name = "geocoded_at")
    private Instant geocodedAt;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingLotStatus status;

    @Column(name = "activation_requested_at")
    private Instant activationRequestedAt;

    @Column(name = "activation_requested_by")
    private UUID activationRequestedBy;

    @OneToMany(mappedBy = "parkingLot")
    private Set<ZoneEntity> zones = new HashSet<>();

    @OneToMany(mappedBy = "parkingLot")
    private Set<DeviceEntity> devices = new HashSet<>();

    @OneToMany(mappedBy = "parkingLot")
    private Set<ShiftEntity> shifts = new HashSet<>();
}
