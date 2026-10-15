package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.WalletTopupOrderEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTopupOrderRepository extends JpaRepository<WalletTopupOrderEntity, UUID> {

    Optional<WalletTopupOrderEntity> findByTransactionRef(String transactionRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WalletTopupOrderEntity t where t.transactionRef = :transactionRef")
    Optional<WalletTopupOrderEntity> findByTransactionRefForUpdate(@Param("transactionRef") String transactionRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WalletTopupOrderEntity t where t.topupOrderId = :id")
    Optional<WalletTopupOrderEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<WalletTopupOrderEntity> findByIdempotencyKey(String idempotencyKey);

    List<WalletTopupOrderEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    @Modifying
    @Query(value = """
            UPDATE billing.wallet_topup_orders SET status = 'EXPIRED', updated_at = now()
            WHERE topup_order_id IN (
                SELECT topup_order_id FROM billing.wallet_topup_orders
                WHERE status = 'PENDING' AND expires_at <= :now
                ORDER BY expires_at ASC LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            ) RETURNING topup_order_id
            """, nativeQuery = true)
    List<UUID> claimExpiredPending(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Query("select t from WalletTopupOrderEntity t where t.status = 'PENDING' and t.expiresAt > :now order by t.createdAt desc")
    List<WalletTopupOrderEntity> findPendingReconciliation(@Param("now") Instant now, Pageable pageable);
}
