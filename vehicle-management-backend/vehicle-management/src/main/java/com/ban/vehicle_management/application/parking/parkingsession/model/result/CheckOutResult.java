package com.ban.vehicle_management.application.parking.parkingsession.model.result;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;

public record CheckOutResult(
        ParkingSession parkingSession,
        ParkingEvent parkingEvent,
        Invoice invoice,
        String customerType,
        String barrierAction
) {
}
