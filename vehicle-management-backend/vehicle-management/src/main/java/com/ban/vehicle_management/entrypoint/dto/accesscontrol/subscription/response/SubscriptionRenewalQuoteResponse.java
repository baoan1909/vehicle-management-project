package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRenewalQuoteResponse(
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal ticketAmount,
        BigDecimal physicalCardDeposit,
        String voucherCode,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        boolean reusesExistingCard,
        boolean physicalCardRequestPending
) {
}
