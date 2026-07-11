package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingSessionManagementResponse {

    private UUID parkingSessionId;
    private UUID cardId;
    private String cardNumber;
    private String cardUid;
    private String cardTypeCode;
    private String cardTypeName;
    private UUID customerId;
    private UUID customerVehicleId;
    private UUID vehicleTypeId;
    private String vehicleTypeCode;
    private String vehicleTypeName;
    private UUID zoneId;
    private String zoneCode;
    private String zoneName;
    private UUID parkingLotId;
    private String parkingLotCode;
    private String parkingLotName;
    private String licensePlateIn;
    private String licensePlateOut;
    private String checkInTime;
    private String checkOutTime;
    private ParkingSessionStatus status;
    private BigDecimal totalPrice;
    private List<EventResponse> events;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EventResponse {
        private UUID parkingEventId;
        private UUID parkingSessionId;
        private UUID laneId;
        private String laneCode;
        private String laneName;
        private ParkingEventType eventType;
        private String eventTime;
        private String licensePlateDetected;
        private String licensePlateImagePath;
        private String personImagePath;
        private UUID actorAccountId;
        private String note;
    }
}
