package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response;

import com.ban.vehicle_management.entrypoint.dto.parking.parkingevent.response.ParkingEventResponse;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingSessionCheckInResponse {

    private ParkingSessionResponse parkingSession;
    private ParkingEventResponse parkingEvent;
    private UUID subscriptionId;
    private String customerType;
    private String barrierAction;
}
