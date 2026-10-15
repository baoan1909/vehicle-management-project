package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Customer-facing invoice view. Internal audit fields ({@code customerId},
 * {@code createdBy}, {@code updatedBy}) and plate normalization snapshots are
 * omitted; the customer scope always comes from the authenticated account.
 */
public record InvoiceUserResponse(
        UUID invoiceId
        , UUID parkingLotId
        , String invoiceNo
        , UUID parkingSessionId
        , UUID subscriptionId
        , UUID lostCardReportId
        , BigDecimal amount
        , BigDecimal discountAmount
        , BigDecimal finalAmount
        , InvoiceStatus status
        , Instant issuedAt
        , Instant paidAt
        , String licensePlateDisplaySnapshot
        , Instant createdAt
        , Instant updatedAt
        ){
}
