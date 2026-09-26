package com.ban.vehicle_management.entrypoint.dto.accesscontrol.lostcardreport.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record LostCardReportListItemResponse(
        UUID lostCardReportId,
        String reportCode,
        UUID cardId,
        UUID customerId,
        UUID parkingSessionId,
        UUID subscriptionId,
        String licensePlate,
        Instant notificationTime,
        Instant timeOfLost,
        BigDecimal ticketPrice,
        BigDecimal lostCardFee,
        BigDecimal totalAmount,
        String reporterName,
        String reporterPhone,
        String identifyCard,
        String registrationLicense,
        LostCardReportContext context,
        LostCardReportStatus status,
        UUID invoiceId,
        String invoiceNo,
        InvoiceStatus invoiceStatus,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy
) {
}
