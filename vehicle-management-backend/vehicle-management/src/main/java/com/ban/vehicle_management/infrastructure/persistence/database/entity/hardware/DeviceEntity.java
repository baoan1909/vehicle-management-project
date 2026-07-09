package com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotEntity;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "devices", schema = "hardware")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEntity extends AuditableEntity {

    @Id
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_lot_id", referencedColumnName = "parking_lot_id", insertable = false, updatable = false)
    private ParkingLotEntity parkingLot;

    @Column(name = "lane_id")
    private UUID laneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", referencedColumnName = "lane_id", insertable = false, updatable = false)
    private LaneEntity lane;

    @Column(name = "device_code", nullable = false, unique = true)
    private String deviceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "ip_address")
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeviceStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

}


