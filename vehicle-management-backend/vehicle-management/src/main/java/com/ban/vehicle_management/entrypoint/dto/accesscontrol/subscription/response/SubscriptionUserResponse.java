package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer-facing subscription view. Internal workflow actors
 * ({@code approvedBy}, {@code rejectedBy}, {@code createdBy}, {@code updatedBy})
 * are omitted; the customer scope always comes from the authenticated account.
 */
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionUserResponse {

    private UUID subscriptionId;
    private UUID parkingLotId;
    private UUID customerVehicleId;
    private UUID cardId;
    private UUID ticketTypeId;
    private UUID priceRuleId;
    private Integer periodQuantity;
    private Boolean physicalCardRequested;
    private BigDecimal physicalCardDeposit;
    private LocalDate requestedEffectiveFrom;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal price;
    private String requestedVoucherCode;
    private SubscriptionStatus status;
    private String rejectionReason;
    private LocalDate cardReceiptDate;
    private Boolean accessBlocked;
    private Instant accessBlockedAt;
    private Instant accessRestoredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
