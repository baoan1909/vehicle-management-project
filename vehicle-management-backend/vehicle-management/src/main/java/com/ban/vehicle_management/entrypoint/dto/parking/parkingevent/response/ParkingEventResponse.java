package com.ban.vehicle_management.entrypoint.dto.parking.parkingevent.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingEventResponse {

    private UUID parkingEventId;
    private UUID parkingSessionId;
    private UUID laneId;
    private ParkingEventType eventType;
    private java.time.Instant eventTime;
    private String licensePlateDetected;
    private String licensePlateDetectedNormalized;
    private String licensePlateDetectedDisplay;
    private String licensePlateFormat;
    private Boolean validFormat;
    private Boolean needsReview;
    private String licensePlateImagePath;
    private String personImagePath;
    private UUID actorAccountId;
    private String note;
}
