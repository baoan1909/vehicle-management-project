package com.ban.vehicle_management.infrastructure.persistence.billing.invoice;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.InvoiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoices", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEntity extends AuditableEntity {

    @Id
    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "invoice_no", nullable = false, unique = true)
    private String invoiceNo;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "parking_session_id")
    private UUID parkingSessionId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "lost_card_report_id")
    private UUID lostCardReportId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

}
