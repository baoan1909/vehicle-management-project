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
    private String licensePlateInNormalized;
    private String licensePlateInDisplay;
    private String licensePlateInFormat;
    private Boolean licensePlateInValidFormat;
    private Boolean licensePlateInNeedsReview;
    private String licensePlateOutNormalized;
    private String licensePlateOutDisplay;
    private String licensePlateOutFormat;
    private Boolean licensePlateOutValidFormat;
    private Boolean licensePlateOutNeedsReview;
    private java.time.Instant checkInTime;
    private java.time.Instant checkOutTime;
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
}
