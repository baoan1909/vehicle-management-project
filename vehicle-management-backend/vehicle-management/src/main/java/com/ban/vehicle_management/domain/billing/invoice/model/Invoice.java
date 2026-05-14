package com.ban.vehicle_management.domain.billing.invoice.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends AuditableDomainModel {

    private UUID invoiceId;
    private String invoiceNo;
    private UUID customerId;
    private UUID parkingSessionId;
    private UUID subscriptionId;
    private UUID lostCardReportId;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private InvoiceStatus status;
    private Instant issuedAt;
    private Instant paidAt;
}
