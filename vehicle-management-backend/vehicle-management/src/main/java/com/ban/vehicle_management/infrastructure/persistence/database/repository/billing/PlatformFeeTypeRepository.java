package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeTypeEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformFeeTypeRepository
        extends JpaRepository<PlatformFeeTypeEntity, UUID>, JpaSpecificationExecutor<PlatformFeeTypeEntity> {

    Optional<PlatformFeeTypeEntity> findByFeeCode(String feeCode);

    boolean existsByFeeCode(String feeCode);

    List<PlatformFeeTypeEntity> findByScopeTypeAndTransactionTypeAndStatus(
            PlatformFeeScopeType scopeType, PlatformFeeTransactionType transactionType, PlatformFeeTypeStatus status);

    List<PlatformFeeTypeEntity> findByScopeTypeAndStatus(
            PlatformFeeScopeType scopeType, PlatformFeeTypeStatus status);

    List<PlatformFeeTypeEntity> findByStatus(PlatformFeeTypeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fee FROM PlatformFeeTypeEntity fee WHERE fee.platformFeeTypeId = :feeTypeId")
    Optional<PlatformFeeTypeEntity> findByIdForUpdate(@Param("feeTypeId") UUID feeTypeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT fee FROM PlatformFeeTypeEntity fee
            WHERE fee.scopeType = :scopeType
              AND ((:transactionType IS NULL AND fee.transactionType IS NULL)
                    OR fee.transactionType = :transactionType)
            ORDER BY fee.platformFeeTypeId
            """)
    List<PlatformFeeTypeEntity> findAllByScopeForUpdate(
            @Param("scopeType") PlatformFeeScopeType scopeType,
            @Param("transactionType") PlatformFeeTransactionType transactionType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT fee FROM PlatformFeeTypeEntity fee
            WHERE fee.status = com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus.ACTIVE
            ORDER BY fee.platformFeeTypeId
            """)
    List<PlatformFeeTypeEntity> findAllActiveForUpdate();
}
