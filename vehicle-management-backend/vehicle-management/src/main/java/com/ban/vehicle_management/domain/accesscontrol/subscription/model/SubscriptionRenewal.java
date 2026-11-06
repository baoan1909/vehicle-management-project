package com.ban.vehicle_management.domain.accesscontrol.subscription.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionRenewalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRenewal extends AuditableDomainModel {
    private UUID renewalId;
    private UUID subscriptionId;
    private UUID invoiceId;
    private UUID ticketTypeId;
    private UUID priceRuleId;
    private Integer periodQuantity;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal ticketAmount;
    private BigDecimal physicalCardDeposit;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String voucherCode;
    private Boolean physicalCardRequested;
    private SubscriptionRenewalStatus status;
    private String idempotencyKey;
    private Instant appliedAt;
}
