package com.ban.vehicle_management.infrastructure.persistence.people.customervehicle;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.CustomerVehicleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_vehicles", schema = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVehicleEntity extends AuditableEntity {

    @Id
    @Column(name = "customer_vehicle_id", nullable = false)
    private UUID customerVehicleId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "vehicle_type_id", nullable = false)
    private UUID vehicleTypeId;

    @Column(name = "license_plate", nullable = false, unique = true)
    private String licensePlate;

    @Column(name = "brand")
    private String brand;

    @Column(name = "color")
    private String color;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerVehicleStatus status;

}
