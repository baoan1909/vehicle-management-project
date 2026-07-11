package com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LostCardReportListItemResult(
        UUID lostCardReportId,
        UUID cardId,
        UUID customerId,
        UUID parkingSessionId,
        UUID subscriptionId,
        String licensePlate,
        Instant notificationTime,
        Instant timeOfLost,
        BigDecimal ticketPrice,
        BigDecimal lostCardFee,
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

    public BigDecimal totalAmount() {
        BigDecimal ticket = ticketPrice == null ? BigDecimal.ZERO : ticketPrice;
        BigDecimal lostFee = lostCardFee == null ? BigDecimal.ZERO : lostCardFee;
        return ticket.add(lostFee);
    }
}
