package com.ban.vehicle_management.entrypoint.dto.catalog.voucher.request;

import com.ban.vehicle_management.shared.enumeration.catalog.VoucherDiscountType;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherScopeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateVoucherRequest(
        VoucherScopeType scopeType,
        UUID organizationId,
        UUID parkingLotId,
        String code,
        String name,
        String description,
        VoucherDiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minimumSubscriptionAmount,
        Integer maxRedemptions,
        Integer maxRedemptionsPerCustomer,
        Instant validFrom,
        Instant validTo,
        boolean showOnDashboard,
        boolean showOnSubscriptionPage,
        String bannerTitle,
        String bannerDescription,
        Integer bannerPriority
) {
}
