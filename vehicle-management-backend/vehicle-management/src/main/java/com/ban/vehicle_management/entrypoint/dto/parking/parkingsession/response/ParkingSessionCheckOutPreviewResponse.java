package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response;

import com.ban.vehicle_management.entrypoint.dto.parking.parkingevent.response.ParkingEventResponse;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingSessionCheckOutPreviewResponse {

    private ParkingSessionResponse parkingSession;
    private ParkingEventResponse checkInEvent;
    private BigDecimal estimatedTotalPrice;
    private String previewCheckOutTime;
    private String customerType;
    private String pricingMessage;
}
