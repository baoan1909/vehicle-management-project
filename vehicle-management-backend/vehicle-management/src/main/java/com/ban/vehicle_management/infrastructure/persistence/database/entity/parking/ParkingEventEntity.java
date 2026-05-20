package com.ban.vehicle_management.infrastructure.persistence.database.entity.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.LaneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_session_id", referencedColumnName = "parking_session_id", insertable = false, updatable = false)
    private ParkingSessionEntity parkingSession;

    @Column(name = "lane_id", nullable = false)
    private UUID laneId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lane_id", referencedColumnName = "lane_id", insertable = false, updatable = false)
    private LaneEntity lane;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_account_id", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity actorAccount;

    @Column(name = "note")
    private String note;

}


