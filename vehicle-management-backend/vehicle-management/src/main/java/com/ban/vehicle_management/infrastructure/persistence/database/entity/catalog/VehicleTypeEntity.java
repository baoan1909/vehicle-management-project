package com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "vehicle_types", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTypeEntity extends AuditableEntity {

    @Id
    @Column(name = "vehicle_type_id", nullable = false)
    private UUID vehicleTypeId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "vehicleType")
    private Set<CustomerVehicleEntity> customerVehicles = new HashSet<>();

    @OneToMany(mappedBy = "vehicleType")
    private Set<PriceRuleEntity> priceRules = new HashSet<>();

    @OneToMany(mappedBy = "vehicleType")
    private Set<ZoneEntity> zones = new HashSet<>();

    @OneToMany(mappedBy = "vehicleType")
    private Set<ParkingSessionEntity> parkingSessions = new HashSet<>();

}


