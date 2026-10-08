package com.ban.vehicle_management.application.billing.wallet.port.out;

import com.ban.vehicle_management.domain.billing.wallet.model.WalletAdjustment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletAdjustmentPortOut {

    WalletAdjustment save(WalletAdjustment adjustment);

    Optional<WalletAdjustment> findById(UUID adjustmentId);

    Optional<WalletAdjustment> findByIdForUpdate(UUID adjustmentId);

    List<WalletAdjustment> findByWallet(UUID walletId);
}
