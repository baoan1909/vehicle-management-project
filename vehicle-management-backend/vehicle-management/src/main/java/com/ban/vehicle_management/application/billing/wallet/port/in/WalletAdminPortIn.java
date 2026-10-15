package com.ban.vehicle_management.application.billing.wallet.port.in;

import com.ban.vehicle_management.domain.billing.wallet.model.WalletAdjustment;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletAdminPortIn {

    void lockWallet(UUID walletId);

    void unlockWallet(UUID walletId);

    WalletAdjustment requestAdjustment(UUID walletId, BigDecimal amount, String direction, String reason);

    WalletAdjustment approveAdjustment(UUID adjustmentId);

    WalletAdjustment rejectAdjustment(UUID adjustmentId, String reason);

    List<WalletAdjustment> listAdjustments(UUID walletId);
}
