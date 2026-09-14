package com.ban.vehicle_management.application.catalog.voucher.port.in;

import com.ban.vehicle_management.application.catalog.voucher.model.result.VoucherQuote;
import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VoucherPortIn {

    Voucher createVoucher(Voucher voucher);
    Voucher updateVoucher(UUID voucherId, Voucher voucher);
    Voucher getVoucherById(UUID voucherId);
    List<Voucher> getVouchers();
    List<Voucher> getCustomerVisibleVouchers(Instant now);
    Voucher activateVoucher(UUID voucherId);
    Voucher pauseVoucher(UUID voucherId);
    VoucherQuote quoteSubscriptionVoucher(String voucherCode, UUID customerId, UUID ticketTypeId, BigDecimal subscriptionAmount, Instant now);
    VoucherQuote reserveSubscriptionVoucher(String voucherCode, UUID customerId, UUID ticketTypeId, BigDecimal subscriptionAmount, UUID subscriptionId, UUID invoiceId, Instant now);
    void redeemSubscriptionVoucher(UUID subscriptionId, Instant redeemedAt);
    void releaseSubscriptionVoucher(UUID subscriptionId, String reason, Instant releasedAt);
}
