package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.WalletAdjustmentEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletAdjustmentRepository extends JpaRepository<WalletAdjustmentEntity, UUID> {

    List<WalletAdjustmentEntity> findByWalletIdOrderByRequestedAtDesc(UUID walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from WalletAdjustmentEntity a where a.walletAdjustmentId = :adjustmentId")
    Optional<WalletAdjustmentEntity> findByIdForUpdate(@Param("adjustmentId") UUID adjustmentId);
}
