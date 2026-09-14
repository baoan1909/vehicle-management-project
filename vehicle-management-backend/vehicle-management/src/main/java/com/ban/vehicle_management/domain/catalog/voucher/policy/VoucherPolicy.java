package com.ban.vehicle_management.domain.catalog.voucher.policy;

import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherDiscountType;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;

public class VoucherPolicy {

    public void initialize(Voucher voucher) {
        requireVoucher(voucher);
        voucher.setCode(TextValidationUtils.normalizeCode(voucher.getCode(), "code", 50));
        voucher.setName(TextValidationUtils.normalizeRequiredText(voucher.getName(), "name", 150));
        voucher.setDescription(TextValidationUtils.normalizeNullableText(voucher.getDescription(), "description", 2000));
        voucher.setBannerTitle(TextValidationUtils.normalizeNullableText(voucher.getBannerTitle(), "bannerTitle", 150));
        voucher.setBannerDescription(TextValidationUtils.normalizeNullableText(voucher.getBannerDescription(), "bannerDescription", 500));
        requireField(voucher.getDiscountType(), "discountType");
        requirePositive(voucher.getDiscountValue(), "discountValue");
        requireNonNegative(voucher.getMinimumSubscriptionAmount(), "minimumSubscriptionAmount");
        requireField(voucher.getValidFrom(), "validFrom");
        requireField(voucher.getValidTo(), "validTo");
        if (!voucher.getValidTo().isAfter(voucher.getValidFrom())) {
            throw new BadRequestException("Voucher validTo must be after validFrom");
        }
        if (voucher.getDiscountType() == VoucherDiscountType.PERCENTAGE
                && voucher.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("Percentage voucher discountValue must not exceed 100");
        }
        if (voucher.getMaxDiscountAmount() != null) requireNonNegative(voucher.getMaxDiscountAmount(), "maxDiscountAmount");
        if (voucher.getMaxRedemptions() != null && voucher.getMaxRedemptions() <= 0) {
            throw new BadRequestException("maxRedemptions must be positive");
        }
        if (voucher.getMaxRedemptionsPerCustomer() == null || voucher.getMaxRedemptionsPerCustomer() <= 0) {
            throw new BadRequestException("maxRedemptionsPerCustomer must be positive");
        }
        if (voucher.getBannerPriority() == null) voucher.setBannerPriority(0);
        if (voucher.getBannerPriority() < 0) throw new BadRequestException("bannerPriority must not be negative");
        if ((voucher.isShowOnDashboard() || voucher.isShowOnSubscriptionPage()) && voucher.getBannerTitle() == null) {
            throw new BadRequestException("bannerTitle is required when customer banner display is enabled");
        }
        if (voucher.getStatus() == null) voucher.setStatus(VoucherStatus.DRAFT);
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal subscriptionAmount) {
        requireVoucher(voucher);
        requireNonNegative(subscriptionAmount, "subscriptionAmount");
        BigDecimal calculated = voucher.getDiscountType() == VoucherDiscountType.PERCENTAGE
                ? subscriptionAmount.multiply(voucher.getDiscountValue()).divide(new BigDecimal("100"))
                : voucher.getDiscountValue();
        if (voucher.getMaxDiscountAmount() != null) calculated = calculated.min(voucher.getMaxDiscountAmount());
        return calculated.min(subscriptionAmount);
    }

    public void ensureApplicable(Voucher voucher, BigDecimal subscriptionAmount, Instant now) {
        requireVoucher(voucher);
        requireField(now, "now");
        if (voucher.getStatus() != VoucherStatus.ACTIVE) throw new BadRequestException("Voucher is not active");
        if (now.isBefore(voucher.getValidFrom()) || !now.isBefore(voucher.getValidTo())) {
            throw new BadRequestException("Voucher is not valid at this time");
        }
        if (subscriptionAmount.compareTo(voucher.getMinimumSubscriptionAmount()) < 0) {
            throw new BadRequestException("Subscription amount does not meet voucher minimum amount");
        }
    }

    public boolean shouldShowToCustomer(Voucher voucher, Instant now) {
        requireVoucher(voucher);
        requireField(now, "now");
        return (voucher.isShowOnDashboard() || voucher.isShowOnSubscriptionPage())
                && voucher.getStatus() == VoucherStatus.ACTIVE
                && !now.isBefore(voucher.getValidFrom())
                && now.isBefore(voucher.getValidTo());
    }

    private void requireVoucher(Voucher voucher) { requireField(voucher, "voucher"); }
    private void requireField(Object value, String name) { if (value == null) throw new BadRequestException(name + " must not be null"); }
    private void requirePositive(BigDecimal value, String name) { requireNonNegative(value, name); if (value.signum() <= 0) throw new BadRequestException(name + " must be positive"); }
    private void requireNonNegative(BigDecimal value, String name) { requireField(value, name); if (value.signum() < 0) throw new BadRequestException(name + " must not be negative"); }
}
