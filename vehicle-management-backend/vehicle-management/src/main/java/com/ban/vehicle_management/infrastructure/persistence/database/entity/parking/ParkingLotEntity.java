package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
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

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingLotStatus status;

    @OneToMany(mappedBy = "parkingLot")
    private Set<ZoneEntity> zones = new HashSet<>();

    @OneToMany(mappedBy = "parkingLot")
    private Set<LaneEntity> lanes = new HashSet<>();

    @OneToMany(mappedBy = "parkingLot")
    private Set<DeviceEntity> devices = new HashSet<>();

    @OneToMany(mappedBy = "parkingLot")
    private Set<ShiftEntity> shifts = new HashSet<>();

}


