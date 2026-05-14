package com.ban.vehicle_management.infrastructure.persistence.parking.zone;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "zones", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZoneEntity extends AuditableEntity {

    @Id
    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "vehicle_type_id")
    private UUID vehicleTypeId;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

}
