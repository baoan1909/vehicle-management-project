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
    private String licensePlateIn;
    private String licensePlateOut;
    private String checkInTime;
    private String checkOutTime;
    private ParkingSessionStatus status;
    private BigDecimal totalPrice;
}
