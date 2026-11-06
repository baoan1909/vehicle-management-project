package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response;

import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionRenewalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionRenewalDetailResponse(
        UUID renewalId,
        UUID subscriptionId,
        UUID invoiceId,
        UUID ticketTypeId,
        UUID priceRuleId,
        Integer periodQuantity,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal ticketAmount,
        BigDecimal physicalCardDeposit,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String voucherCode,
        Boolean physicalCardRequested,
        SubscriptionRenewalStatus status
) {
}
