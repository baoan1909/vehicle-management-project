package com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionRenewalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_renewals", schema = "access_control")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRenewalEntity extends AuditableEntity {
    @Id
    @Column(name = "renewal_id", nullable = false)
    private UUID renewalId;
    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;
    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;
    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;
    @Column(name = "price_rule_id", nullable = false)
    private UUID priceRuleId;
    @Column(name = "period_quantity", nullable = false)
    private Integer periodQuantity;
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;
    @Column(name = "ticket_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal ticketAmount;
    @Column(name = "physical_card_deposit", nullable = false, precision = 12, scale = 2)
    private BigDecimal physicalCardDeposit;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount;
    @Column(name = "voucher_code", length = 50)
    private String voucherCode;
    @Column(name = "physical_card_requested", nullable = false)
    private Boolean physicalCardRequested;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionRenewalStatus status;
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;
    @Column(name = "applied_at")
    private Instant appliedAt;
}
