package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response;

import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingevent.response.ParkingEventResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingSessionCheckOutResponse {

    private ParkingSessionResponse parkingSession;
    private ParkingEventResponse parkingEvent;
    private InvoiceAdminResponse invoice;
    private String customerType;
    private String barrierAction;
}
