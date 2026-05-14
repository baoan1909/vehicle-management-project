package com.ban.vehicle_management.infrastructure.persistence.billing.payment;

import com.ban.vehicle_management.shared.enumeration.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.PaymentStatus;
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

    @Column(name = "note")
    private String note;

}
