package com.ban.vehicle_management.application.catalog.voucher.port.out;

import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;

public interface VoucherFundingPortOut {
    VoucherFinancialTerm saveFundingTerm(VoucherFinancialTerm term);
}
