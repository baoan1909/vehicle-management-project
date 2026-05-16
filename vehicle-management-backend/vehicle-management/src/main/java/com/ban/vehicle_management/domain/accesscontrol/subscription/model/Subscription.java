package com.ban.vehicle_management.domain.accesscontrol.subscription.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.SubscriptionStatus;
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
public class Subscription extends AuditableDomainModel {

    private UUID subscriptionId;
    private UUID customerId;
    private UUID customerVehicleId;
    private UUID cardId;
    private UUID ticketTypeId;
    private UUID priceRuleId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal price;
    private SubscriptionStatus status;
    private UUID approvedBy;
    private Instant approvedAt;
    private LocalDate cardReceiptDate;
}

