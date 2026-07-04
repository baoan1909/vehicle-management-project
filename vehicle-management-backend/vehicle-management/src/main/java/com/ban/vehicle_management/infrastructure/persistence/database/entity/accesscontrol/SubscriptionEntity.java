package com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.CardEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.InvoiceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.TicketTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.CustomerVehicleEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
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
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscriptions", schema = "access_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEntity extends AuditableEntity {

    @Id
    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", referencedColumnName = "customer_id", insertable = false, updatable = false)
    private CustomerEntity customer;

    @Column(name = "customer_vehicle_id", nullable = false)
    private UUID customerVehicleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_vehicle_id", referencedColumnName = "customer_vehicle_id", insertable = false, updatable = false)
    private CustomerVehicleEntity customerVehicle;

    @Column(name = "card_id")
    private UUID cardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", referencedColumnName = "card_id", insertable = false, updatable = false)
    private CardEntity card;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", referencedColumnName = "ticket_type_id", insertable = false, updatable = false)
    private TicketTypeEntity ticketType;

    @Column(name = "price_rule_id")
    private UUID priceRuleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_rule_id", referencedColumnName = "price_rule_id", insertable = false, updatable = false)
    private PriceRuleEntity priceRule;

    @Column(name = "requested_effective_from", nullable = false)
    private LocalDate requestedEffectiveFrom;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity approvedByAccount;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity rejectedByAccount;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "card_receipt_date")
    private LocalDate cardReceiptDate;

    @OneToMany(mappedBy = "subscription")
    private Set<InvoiceEntity> invoices = new HashSet<>();

}


