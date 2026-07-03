package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record LostCardReportResponse(
        UUID lostCardReportId,
        UUID cardId,
        UUID customerId,
        UUID parkingSessionId,
        UUID subscriptionId,
        String notificationTime,
        String timeOfLost,
        BigDecimal ticketPrice,
        BigDecimal lostCardFee,
        String reporterName,
        String reporterPhone,
        String identifyCard,
        String registrationLicense,
        String note,
        LostCardReportContext context,
        LostCardReportStatus status,
        UUID resolvedBy,
        String resolvedAt,
        UUID cancelledBy,
        String cancelledAt,
        String cancelReason,
        String createdAt,
        UUID createdBy,
        String updatedAt,
        UUID updatedBy
) {
}