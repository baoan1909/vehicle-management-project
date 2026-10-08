package com.ban.vehicle_management.application.billing.wallet.port.out;

import com.ban.vehicle_management.domain.billing.wallet.model.WalletTopupOrder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTopupPortOut {

    WalletTopupOrder save(WalletTopupOrder order);

    Optional<WalletTopupOrder> findById(UUID topupOrderId);

    Optional<WalletTopupOrder> findByIdForUpdate(UUID topupOrderId);

    Optional<WalletTopupOrder> findByTransactionRef(String transactionRef);

    Optional<WalletTopupOrder> findByTransactionRefForUpdate(String transactionRef);

    Optional<WalletTopupOrder> findByIdempotencyKey(String idempotencyKey);

    List<WalletTopupOrder> findByWallet(UUID walletId, int page, int size);

    List<UUID> claimExpiredPending(int batchSize, Instant now);

    List<WalletTopupOrder> findReconciliationExceptions(int page, int size);
}
