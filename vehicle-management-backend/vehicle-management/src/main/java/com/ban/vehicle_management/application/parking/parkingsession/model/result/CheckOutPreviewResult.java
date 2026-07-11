package com.ban.vehicle_management.application.parking.parkingsession.model.result;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import java.math.BigDecimal;
import java.time.Instant;

public record CheckOutPreviewResult(
        ParkingSession parkingSession,
        ParkingEvent checkInEvent,
        BigDecimal estimatedTotalPrice,
        Instant previewCheckOutTime,
        String customerType,
        String pricingMessage
) {
}
