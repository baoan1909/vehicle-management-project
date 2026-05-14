package com.ban.vehicle_management.infrastructure.persistence.hardware.device;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.DeviceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
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

    @Column(name = "lane_id")
    private UUID laneId;

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

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

}
