package com.ban.vehicle_management.infrastructure.persistence.billing.invoice;

import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.lostcardreport.LostCardReportEntity;
import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.subscription.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.billing.payment.PaymentEntity;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingsession.ParkingSessionEntity;
import com.ban.vehicle_management.infrastructure.persistence.people.customer.CustomerEntity;
import com.ban.vehicle_management.shared.enumeration.InvoiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", insertable = false, updatable = false)
    private CustomerEntity customer;

    @Column(name = "parking_session_id")
    private UUID parkingSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_session_id", referencedColumnName = "parking_session_id", insertable = false, updatable = false)
    private ParkingSessionEntity parkingSession;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", referencedColumnName = "subscription_id", insertable = false, updatable = false)
    private SubscriptionEntity subscription;

    @Column(name = "lost_card_report_id")
    private UUID lostCardReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_card_report_id", referencedColumnName = "lost_card_report_id", insertable = false, updatable = false)
    private LostCardReportEntity lostCardReport;

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

    @OneToMany(mappedBy = "invoice")
    private Set<PaymentEntity> payments = new HashSet<>();

}
