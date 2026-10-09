package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.WalletEntity;
import com.ban.vehicle_management.shared.enumeration.billing.WalletPurpose;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO billing.wallets (
                wallet_id, owner_type, customer_id, organization_id, wallet_purpose,
                currency, available_balance, pending_balance, held_balance, status,
                version, created_at, created_by
            ) VALUES (
                :walletId, :ownerType, :customerId, :organizationId, :walletPurpose,
                :currency, :availableBalance, :pendingBalance, :heldBalance, :status,
                0, :createdAt, :createdBy
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("walletId") UUID walletId,
            @Param("ownerType") String ownerType,
            @Param("customerId") UUID customerId,
            @Param("organizationId") UUID organizationId,
            @Param("walletPurpose") String walletPurpose,
            @Param("currency") String currency,
            @Param("availableBalance") java.math.BigDecimal availableBalance,
            @Param("pendingBalance") java.math.BigDecimal pendingBalance,
            @Param("heldBalance") java.math.BigDecimal heldBalance,
            @Param("status") String status,
            @Param("createdAt") java.time.Instant createdAt,
            @Param("createdBy") UUID createdBy);

    Optional<WalletEntity> findByCustomerIdAndCurrencyAndWalletPurpose(
            UUID customerId, String currency, WalletPurpose walletPurpose);

    Optional<WalletEntity> findByOrganizationIdAndCurrencyAndWalletPurpose(
            UUID organizationId, String currency, WalletPurpose walletPurpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.walletId = :walletId")
    Optional<WalletEntity> findByIdForUpdate(@Param("walletId") UUID walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.customerId = :customerId and w.currency = :currency and w.walletPurpose = :purpose")
    Optional<WalletEntity> findCustomerWalletForUpdate(
            @Param("customerId") UUID customerId,
            @Param("currency") String currency,
            @Param("purpose") WalletPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.organizationId = :organizationId and w.currency = :currency and w.walletPurpose = :purpose")
    Optional<WalletEntity> findOrganizationWalletForUpdate(
            @Param("organizationId") UUID organizationId,
            @Param("currency") String currency,
            @Param("purpose") WalletPurpose purpose);

    Page<WalletEntity> findAll(Pageable pageable);
}
