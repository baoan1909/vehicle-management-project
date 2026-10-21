package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.FinancialTransactionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransactionEntity, UUID> {

    Optional<FinancialTransactionEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<FinancialTransactionEntity> findByTransactionCode(String transactionCode);

    @Query("select t from FinancialTransactionEntity t where t.referenceType = :referenceType and t.referenceId = :referenceId order by t.createdAt desc")
    Page<FinancialTransactionEntity> findByWalletReference(
            @Param("referenceType") String referenceType,
            @Param("referenceId") UUID referenceId,
            Pageable pageable);
}
