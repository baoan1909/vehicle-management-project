package com.ban.vehicle_management.infrastructure.persistence.parking.parkingevent;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.ParkingEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parking_events", schema = "parking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingEventEntity extends AuditableEntity {

    @Id
    @Column(name = "parking_event_id", nullable = false)
    private UUID parkingEventId;

    @Column(name = "parking_session_id", nullable = false)
    private UUID parkingSessionId;

    @Column(name = "lane_id", nullable = false)
    private UUID laneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ParkingEventType eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "license_plate_detected")
    private String licensePlateDetected;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "actor_account_id")
    private UUID actorAccountId;

    @Column(name = "note")
    private String note;

}
