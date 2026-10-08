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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

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
