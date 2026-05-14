package com.ban.vehicle_management.infrastructure.persistence.parking.parkingspace;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.ParkingSpaceStatus;
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

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingSpaceStatus status;

}
