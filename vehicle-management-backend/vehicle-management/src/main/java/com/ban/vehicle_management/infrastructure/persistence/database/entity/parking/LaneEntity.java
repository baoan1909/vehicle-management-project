package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
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
import java.util.HashSet;
import java.util.Set;
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

    @Column(name = "gate_id", nullable = false)
    private UUID gateId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gate_id", referencedColumnName = "gate_id", insertable = false, updatable = false)
    private GateEntity gate;

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

    @OneToMany(mappedBy = "lane")
    private Set<DeviceEntity> devices = new HashSet<>();

    @OneToMany(mappedBy = "lane")
    private Set<ParkingEventEntity> parkingEvents = new HashSet<>();

}


