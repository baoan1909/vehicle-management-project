package com.ban.vehicle_management.domain.parking.parkingevent.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingEvent extends AuditableDomainModel {

    private UUID parkingEventId;
    private UUID parkingSessionId;
    private UUID laneId;
    private ParkingEventType eventType;
    private Instant eventTime;
    private String licensePlateDetected;
    private String licensePlateImagePath;
    private String personImagePath;
    private UUID actorAccountId;
    private String note;
}

