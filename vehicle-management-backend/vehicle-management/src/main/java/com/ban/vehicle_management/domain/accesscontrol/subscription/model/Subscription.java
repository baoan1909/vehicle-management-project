package com.ban.vehicle_management.domain.accesscontrol.subscription.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
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
    private UUID parkingLotId;
    private UUID customerId;
    private UUID customerVehicleId;
    private UUID cardId;
    private UUID ticketTypeId;
    private UUID priceRuleId;
    private Integer periodQuantity = 1;
    private Boolean physicalCardRequested = true;
    private BigDecimal physicalCardDeposit = BigDecimal.ZERO;
    private LocalDate requestedEffectiveFrom;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal price;
    private String requestedVoucherCode;
    private SubscriptionStatus status;
    private UUID approvedBy;
    private Instant approvedAt;
    private String rejectionReason;
    private UUID rejectedBy;
    private Instant rejectedAt;
    private LocalDate cardReceiptDate;
    private String idempotencyKey;
    private String qrAccessToken;
    private Instant qrIssuedAt;
    private Instant qrRevokedAt;
    private Boolean accessBlocked = false;
    private Instant accessBlockedAt;
    private UUID accessBlockedBy;
    private Instant accessRestoredAt;
    private UUID accessRestoredBy;
}
