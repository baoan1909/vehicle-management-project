package com.ban.vehicle_management.infrastructure.persistence.catalog.vehicletype;

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

}
