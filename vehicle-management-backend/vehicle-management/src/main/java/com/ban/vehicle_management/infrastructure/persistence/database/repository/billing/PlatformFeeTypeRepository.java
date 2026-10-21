package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeTypeEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
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

    List<PlatformFeeTypeEntity> findByTransactionTypeAndStatus(
            PlatformFeeTransactionType transactionType, PlatformFeeTypeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fee FROM PlatformFeeTypeEntity fee WHERE fee.platformFeeTypeId = :feeTypeId")
    Optional<PlatformFeeTypeEntity> findByIdForUpdate(@Param("feeTypeId") UUID feeTypeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT fee FROM PlatformFeeTypeEntity fee
            WHERE fee.transactionType = :transactionType
            ORDER BY fee.platformFeeTypeId
            """)
    List<PlatformFeeTypeEntity> findAllByTransactionTypeForUpdate(
            @Param("transactionType") PlatformFeeTransactionType transactionType);
}
