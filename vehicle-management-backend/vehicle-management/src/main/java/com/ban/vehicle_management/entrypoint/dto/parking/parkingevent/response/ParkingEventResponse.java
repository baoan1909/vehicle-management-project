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
    private String eventTime;
    private String licensePlateDetected;
    private String licensePlateImagePath;
    private String personImagePath;
    private UUID actorAccountId;
    private String note;
}
