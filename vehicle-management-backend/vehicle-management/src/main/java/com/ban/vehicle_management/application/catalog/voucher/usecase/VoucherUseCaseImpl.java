package com.ban.vehicle_management.application.catalog.voucher.usecase;

import com.ban.vehicle_management.application.catalog.voucher.model.result.VoucherQuote;
import com.ban.vehicle_management.application.catalog.voucher.port.in.VoucherPortIn;
import com.ban.vehicle_management.application.catalog.voucher.port.out.VoucherPortOut;
import com.ban.vehicle_management.domain.billing.voucherredemption.model.VoucherRedemption;
import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.domain.catalog.voucher.policy.VoucherPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherRedemptionStatus;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherUseCaseImpl implements VoucherPortIn {
    private static final Set<VoucherRedemptionStatus> QUOTA_STATUSES = Set.of(VoucherRedemptionStatus.RESERVED, VoucherRedemptionStatus.REDEEMED);
    private final VoucherPortOut voucherPortOut;
    private final VoucherPolicy voucherPolicy = new VoucherPolicy();

    public VoucherUseCaseImpl(VoucherPortOut voucherPortOut) { this.voucherPortOut = voucherPortOut; }

    @Override @Transactional
    public Voucher createVoucher(Voucher voucher) {
        voucherPolicy.initialize(voucher);
        if (voucherPortOut.findVoucherByCode(voucher.getCode()).isPresent()) throw new ConflictException("Voucher code already exists");
        voucher.setVoucherId(UUID.randomUUID());
        return voucherPortOut.saveVoucher(voucher);
    }

    @Override @Transactional
    public Voucher updateVoucher(UUID voucherId, Voucher changes) {
        Voucher voucher = getVoucherById(voucherId);
        voucher.setCode(changes.getCode());
        voucher.setName(changes.getName());
        voucher.setDescription(changes.getDescription());
        voucher.setDiscountType(changes.getDiscountType());
        voucher.setDiscountValue(changes.getDiscountValue());
        voucher.setMaxDiscountAmount(changes.getMaxDiscountAmount());
        voucher.setMinimumSubscriptionAmount(changes.getMinimumSubscriptionAmount());
        voucher.setMaxRedemptions(changes.getMaxRedemptions());
        voucher.setMaxRedemptionsPerCustomer(changes.getMaxRedemptionsPerCustomer());
        voucher.setValidFrom(changes.getValidFrom());
        voucher.setValidTo(changes.getValidTo());
        voucher.setShowOnDashboard(changes.isShowOnDashboard());
        voucher.setShowOnSubscriptionPage(changes.isShowOnSubscriptionPage());
        voucher.setBannerTitle(changes.getBannerTitle());
        voucher.setBannerDescription(changes.getBannerDescription());
        voucher.setBannerPriority(changes.getBannerPriority());
        voucherPolicy.initialize(voucher);
        if (voucherPortOut.existsVoucherByCodeAndVoucherIdNot(voucher.getCode(), voucherId)) throw new ConflictException("Voucher code already exists");
        return voucherPortOut.saveVoucher(voucher);
    }

    @Override @Transactional(readOnly = true)
    public Voucher getVoucherById(UUID voucherId) { return voucherPortOut.findVoucherById(voucherId).orElseThrow(() -> new NotFoundException("Voucher not found")); }

    @Override @Transactional(readOnly = true)
    public List<Voucher> getVouchers() { return voucherPortOut.findAllVouchers(); }

    @Override @Transactional(readOnly = true)
    public List<Voucher> getCustomerVisibleVouchers(Instant now) {
        return voucherPortOut.findAllVouchers().stream()
                .filter(voucher -> voucherPolicy.shouldShowToCustomer(voucher, now))
                .sorted(java.util.Comparator.comparing(Voucher::getBannerPriority).reversed()
                        .thenComparing(Voucher::getCode))
                .toList();
    }

    @Override @Transactional
    public Voucher activateVoucher(UUID voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        voucher.setStatus(VoucherStatus.ACTIVE);
        voucherPolicy.initialize(voucher);
        return voucherPortOut.saveVoucher(voucher);
    }

    @Override @Transactional
    public Voucher pauseVoucher(UUID voucherId) {
        Voucher voucher = getVoucherById(voucherId);
        voucher.setStatus(VoucherStatus.PAUSED);
        return voucherPortOut.saveVoucher(voucher);
    }

    @Override @Transactional(readOnly = true)
    public VoucherQuote quoteSubscriptionVoucher(String voucherCode, UUID customerId, UUID ticketTypeId, BigDecimal subscriptionAmount, Instant now) {
        if (voucherCode == null || voucherCode.isBlank()) return VoucherQuote.withoutVoucher(subscriptionAmount);
        Voucher voucher = voucherPortOut.findVoucherByCode(normalizeCode(voucherCode)).orElseThrow(() -> new NotFoundException("Voucher code not found"));
        validateApplicableVoucher(voucher, customerId, subscriptionAmount, now, true);
        return toQuote(voucher, subscriptionAmount);
    }

    @Override @Transactional
    public VoucherQuote reserveSubscriptionVoucher(String voucherCode, UUID customerId, UUID ticketTypeId, BigDecimal subscriptionAmount, UUID subscriptionId, UUID invoiceId, Instant now) {
        if (voucherCode == null || voucherCode.isBlank()) return VoucherQuote.withoutVoucher(subscriptionAmount);
        if (voucherPortOut.findRedemptionBySubscriptionId(subscriptionId).isPresent()) throw new ConflictException("Voucher has already been reserved for subscription");
        Voucher voucher = voucherPortOut.findVoucherByCodeForUpdate(normalizeCode(voucherCode)).orElseThrow(() -> new NotFoundException("Voucher code not found"));
        validateApplicableVoucher(voucher, customerId, subscriptionAmount, now, true);
        VoucherQuote quote = toQuote(voucher, subscriptionAmount);
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucherRedemptionId(UUID.randomUUID());
        redemption.setVoucherId(voucher.getVoucherId());
        redemption.setInvoiceId(invoiceId);
        redemption.setSubscriptionId(subscriptionId);
        redemption.setCustomerId(customerId);
        redemption.setVoucherCodeSnapshot(voucher.getCode());
        redemption.setDiscountTypeSnapshot(voucher.getDiscountType());
        redemption.setDiscountValueSnapshot(voucher.getDiscountValue());
        redemption.setDiscountAmount(quote.discountAmount());
        redemption.setStatus(VoucherRedemptionStatus.RESERVED);
        redemption.setReservedAt(now);
        voucherPortOut.saveRedemption(redemption);
        return quote;
    }

    @Override @Transactional
    public void redeemSubscriptionVoucher(UUID subscriptionId, Instant redeemedAt) {
        voucherPortOut.findRedemptionBySubscriptionId(subscriptionId).ifPresent(redemption -> {
            if (redemption.getStatus() == VoucherRedemptionStatus.RESERVED) {
                redemption.setStatus(VoucherRedemptionStatus.REDEEMED);
                redemption.setRedeemedAt(redeemedAt);
                voucherPortOut.saveRedemption(redemption);
            }
        });
    }

    @Override @Transactional
    public void releaseSubscriptionVoucher(UUID subscriptionId, String reason, Instant releasedAt) {
        voucherPortOut.findRedemptionBySubscriptionId(subscriptionId).ifPresent(redemption -> {
            if (redemption.getStatus() == VoucherRedemptionStatus.RESERVED) {
                redemption.setStatus(VoucherRedemptionStatus.RELEASED);
                redemption.setReleasedAt(releasedAt);
                redemption.setReleaseReason(TextValidationUtils.normalizeNullableText(reason, "reason", 500));
                voucherPortOut.saveRedemption(redemption);
            }
        });
    }

    private void validateApplicableVoucher(Voucher voucher, UUID customerId, BigDecimal subscriptionAmount, Instant now, boolean checkQuota) {
        voucherPolicy.ensureApplicable(voucher, subscriptionAmount, now);
        if (!checkQuota) return;
        long total = voucherPortOut.countRedemptionsByVoucherIdAndStatusIn(voucher.getVoucherId(), QUOTA_STATUSES);
        if (voucher.getMaxRedemptions() != null && total >= voucher.getMaxRedemptions()) throw new ConflictException("Voucher usage limit has been reached");
        long customerTotal = voucherPortOut.countRedemptionsByVoucherIdAndCustomerIdAndStatusIn(voucher.getVoucherId(), customerId, QUOTA_STATUSES);
        if (customerTotal >= voucher.getMaxRedemptionsPerCustomer()) throw new ConflictException("Customer has reached the voucher usage limit");
    }

    private VoucherQuote toQuote(Voucher voucher, BigDecimal amount) {
        BigDecimal discount = voucherPolicy.calculateDiscount(voucher, amount);
        return new VoucherQuote(voucher.getVoucherId(), voucher.getCode(), discount, amount.subtract(discount));
    }

    private String normalizeCode(String value) { return TextValidationUtils.normalizeCode(value, "voucherCode", 50); }
}
