package com.ban.vehicle_management.domain.catalog.voucher.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherDiscountType;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VoucherPolicyTest {

    private final VoucherPolicy voucherPolicy = new VoucherPolicy();

    @Test
    void shouldNormalizeAndCalculatePercentageVoucherWithCap() {
        Voucher voucher = validVoucher();
        voucher.setCode(" welcome10 ");
        voucher.setDiscountValue(new BigDecimal("10"));
        voucher.setMaxDiscountAmount(new BigDecimal("15000"));

        voucherPolicy.initialize(voucher);

        assertEquals("WELCOME10", voucher.getCode());
        assertEquals(VoucherStatus.DRAFT, voucher.getStatus());
        assertEquals(new BigDecimal("15000"), voucherPolicy.calculateDiscount(voucher, new BigDecimal("200000")));
    }

    @Test
    void shouldRejectInactiveVoucherWhenApplying() {
        Voucher voucher = validVoucher();
        voucherPolicy.initialize(voucher);

        assertThrows(BadRequestException.class, () -> voucherPolicy.ensureApplicable(
                voucher,
                new BigDecimal("100000"),
                Instant.parse("2026-09-12T02:00:00Z")
        ));
    }

    @Test
    void shouldRejectPercentageAboveOneHundred() {
        Voucher voucher = validVoucher();
        voucher.setDiscountValue(new BigDecimal("101"));

        assertThrows(BadRequestException.class, () -> voucherPolicy.initialize(voucher));
    }

    @Test
    void shouldShowActiveConfiguredBannerWithoutCheckingCustomerRedemptions() {
        Voucher voucher = validVoucher();
        voucher.setStatus(VoucherStatus.ACTIVE);
        voucher.setShowOnDashboard(true);
        voucher.setBannerTitle("Giảm giá vé tháng");

        voucherPolicy.initialize(voucher);

        assertTrue(voucherPolicy.shouldShowToCustomer(voucher, Instant.parse("2026-09-12T02:00:00Z")));
    }

    @Test
    void shouldRequireBannerTitleWhenCustomerDisplayIsEnabled() {
        Voucher voucher = validVoucher();
        voucher.setShowOnSubscriptionPage(true);

        assertThrows(BadRequestException.class, () -> voucherPolicy.initialize(voucher));
    }

    private Voucher validVoucher() {
        Voucher voucher = new Voucher();
        voucher.setCode("WELCOME10");
        voucher.setName("Ưu đãi khách mới");
        voucher.setDiscountType(VoucherDiscountType.PERCENTAGE);
        voucher.setDiscountValue(new BigDecimal("10"));
        voucher.setMinimumSubscriptionAmount(BigDecimal.ZERO);
        voucher.setMaxRedemptionsPerCustomer(1);
        voucher.setValidFrom(Instant.parse("2026-09-01T00:00:00Z"));
        voucher.setValidTo(Instant.parse("2026-10-01T00:00:00Z"));
        return voucher;
    }
}
