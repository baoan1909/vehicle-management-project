package com.ban.vehicle_management.infrastructure.persistence.parking.lane;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.LaneStatus;
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
@Table(name = "lanes", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LaneEntity extends AuditableEntity {

    @Id
    @Column(name = "lane_id", nullable = false)
    private UUID laneId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private LaneDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LaneStatus status;

}
