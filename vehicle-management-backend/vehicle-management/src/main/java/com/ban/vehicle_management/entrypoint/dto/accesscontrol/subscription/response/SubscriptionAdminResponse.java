package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionAdminResponse {

    private UUID subscriptionId;
    private UUID customerId;
    private UUID customerVehicleId;
    private UUID cardId;
    private UUID ticketTypeId;
    private UUID priceRuleId;
    private LocalDate requestedEffectiveFrom;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal price;
    private SubscriptionStatus status;
    private UUID approvedBy;
    private String approvedAt;
    private String rejectionReason;
    private UUID rejectedBy;
    private String rejectedAt;
    private LocalDate cardReceiptDate;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}