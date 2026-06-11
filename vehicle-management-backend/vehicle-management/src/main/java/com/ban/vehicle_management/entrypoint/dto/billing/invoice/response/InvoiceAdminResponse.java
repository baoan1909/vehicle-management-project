package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

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
        , String issuedAt
        , String paidAt
        , String createdAt
        , UUID createdBy
        , String updatedAt
        , UUID updatedBy
        ){
}
