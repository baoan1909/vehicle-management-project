package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer-facing parking session view. Internal fields ({@code customerId},
 * card UID, plate normalization/review flags, staff actor linkage and person
 * images) are omitted; only the evidence the customer needs is exposed.
 */
@Getter
@Setter
@NoArgsConstructor
public class ParkingSessionUserResponse {

    private UUID parkingSessionId;
    private UUID cardId;
    private String cardNumber;
    private String cardTypeCode;
    private String cardTypeName;
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
    private String licensePlateInDisplay;
    private String licensePlateOutDisplay;
    private Instant checkInTime;
    private Instant checkOutTime;
    private ParkingSessionStatus status;
    private BigDecimal totalPrice;
    private List<EventUserResponse> events;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EventUserResponse {
        private UUID parkingEventId;
        private UUID parkingSessionId;
        private UUID laneId;
        private String laneCode;
        private String laneName;
        private ParkingEventType eventType;
        private Instant eventTime;
        private String licensePlateDetected;
        private String licensePlateDetectedDisplay;
        private String licensePlateImagePath;
        private String note;
    }
}
