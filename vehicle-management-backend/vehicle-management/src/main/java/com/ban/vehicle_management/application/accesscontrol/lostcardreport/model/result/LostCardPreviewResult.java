package com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import java.math.BigDecimal;
import java.util.UUID;

public record LostCardPreviewResult(
        LostCardReportContext context,
        ParkingSession parkingSession,
        Subscription subscription,
        UUID cardId,
        UUID customerId,
        UUID customerVehicleId,
        BigDecimal ticketPrice,
        BigDecimal lostCardFee,
        BigDecimal totalAmount,
        String oldCardNumber,
        String customerName,
        String licensePlate,
        String checkInLicensePlateImagePath,
        String checkInPersonImagePath
) {
}
