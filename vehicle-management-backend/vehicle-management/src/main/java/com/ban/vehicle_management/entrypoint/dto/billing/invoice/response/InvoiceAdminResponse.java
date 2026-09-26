package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceAdminResponse(
        UUID invoiceId
        , String invoiceNo
        , UUID customerId
        , UUID parkingSessionId
        , UUID subscriptionId
        , UUID lostCardReportId
        , BigDecimal amount
        , BigDecimal discountAmount
        , BigDecimal finalAmount
        , InvoiceStatus status
        , Instant issuedAt
        , Instant paidAt
        , Instant createdAt
        , UUID createdBy
        , Instant updatedAt
        , UUID updatedBy
        ){
}
