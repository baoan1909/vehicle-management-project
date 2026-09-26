package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingSessionResponse {

    private UUID parkingSessionId;
    private UUID cardId;
    private UUID customerId;
    private UUID customerVehicleId;
    private UUID vehicleTypeId;
    private UUID zoneId;
    private UUID parkingLotId;
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
}
