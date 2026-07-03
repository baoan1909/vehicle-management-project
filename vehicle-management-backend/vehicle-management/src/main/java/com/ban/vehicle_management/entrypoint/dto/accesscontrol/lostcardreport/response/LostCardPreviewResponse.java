package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response;

import com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response.SubscriptionAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionResponse;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import java.math.BigDecimal;
import java.util.UUID;

public record LostCardPreviewResponse(
        LostCardReportContext context,
        ParkingSessionResponse parkingSession,
        SubscriptionAdminResponse subscription,
        UUID cardId,
        UUID customerId,
        UUID customerVehicleId,
        BigDecimal ticketPrice,
        BigDecimal lostCardFee,
        BigDecimal totalAmount,
        String checkInLicensePlateImagePath,
        String checkInPersonImagePath
) {
}