package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response;

import java.time.Instant;

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
        Instant notificationTime,
        Instant timeOfLost,
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
        Instant resolvedAt,
        UUID cancelledBy,
        Instant cancelledAt,
        String cancelReason,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}
