package com.ban.vehicle_management.infrastructure.persistence.accesscontrol.subscription;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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

    @Column(name = "customer_vehicle_id", nullable = false)
    private UUID customerVehicleId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(name = "price_rule_id")
    private UUID priceRuleId;

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

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "card_receipt_date")
    private LocalDate cardReceiptDate;

}
