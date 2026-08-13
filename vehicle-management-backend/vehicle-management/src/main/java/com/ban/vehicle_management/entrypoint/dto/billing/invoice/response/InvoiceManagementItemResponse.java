package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

import com.ban.vehicle_management.shared.enumeration.billing.InvoiceSource;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceManagementItemResponse(
        UUID invoiceId,
        String invoiceNo,
        UUID customerId,
        String customerName,
        String licensePlate,
        InvoiceSource source,
        UUID sourceId,
        BigDecimal amount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        InvoiceStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String transactionRef,
        String issuedAt,
        String paidAt,
        String createdAt,
        String updatedAt
) {
}
