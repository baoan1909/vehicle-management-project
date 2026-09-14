package com.ban.vehicle_management.application.catalog.voucher.port.out;

import com.ban.vehicle_management.domain.billing.voucherredemption.model.VoucherRedemption;
import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherRedemptionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoucherPortOut {

    Voucher saveVoucher(Voucher voucher);
    Optional<Voucher> findVoucherById(UUID voucherId);
    Optional<Voucher> findVoucherByCode(String code);
    Optional<Voucher> findVoucherByCodeForUpdate(String code);
    boolean existsVoucherByCodeAndVoucherIdNot(String code, UUID voucherId);
    List<Voucher> findAllVouchers();
    VoucherRedemption saveRedemption(VoucherRedemption redemption);
    Optional<VoucherRedemption> findRedemptionBySubscriptionId(UUID subscriptionId);
    long countRedemptionsByVoucherIdAndStatusIn(UUID voucherId, Collection<VoucherRedemptionStatus> statuses);
    long countRedemptionsByVoucherIdAndCustomerIdAndStatusIn(UUID voucherId, UUID customerId, Collection<VoucherRedemptionStatus> statuses);
}
