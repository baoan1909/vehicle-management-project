package com.ban.vehicle_management.infrastructure.persistence.billing.payment;

import com.ban.vehicle_management.infrastructure.persistence.billing.invoice.InvoiceEntity;
import com.ban.vehicle_management.infrastructure.persistence.iam.account.AccountEntity;
import com.ban.vehicle_management.shared.enumeration.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {

    @Id
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", referencedColumnName = "invoice_id", insertable = false, updatable = false)
    private InvoiceEntity invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(name = "received_by")
    private UUID receivedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity receivedByAccount;

    @Column(name = "note")
    private String note;

}
