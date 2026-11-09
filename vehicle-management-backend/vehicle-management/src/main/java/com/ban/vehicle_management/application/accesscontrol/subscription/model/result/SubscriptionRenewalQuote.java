package com.ban.vehicle_management.application.accesscontrol.subscription.model.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRenewalQuote(
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
