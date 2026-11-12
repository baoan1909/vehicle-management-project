package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.FinancialTransactionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
import java.util.Collection;
import java.util.List;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransactionEntity, UUID> {

    @Query(value = """
        select distinct transaction.financial_transaction_id as "resourceId",
               ledger_account.organization_id as "organizationId",
               null::uuid as "parkingLotId"
          from billing.financial_transactions transaction
          join billing.ledger_entries entry
            on entry.financial_transaction_id = transaction.financial_transaction_id
          join billing.ledger_accounts ledger_account
            on ledger_account.ledger_account_id = entry.ledger_account_id
         where ledger_account.organization_id in (:organizationIds)
           and transaction.status = 'PENDING'
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findOrganizationDeletionBlockers(
            @Param("organizationIds") Collection<UUID> organizationIds);

    Optional<FinancialTransactionEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<FinancialTransactionEntity> findByTransactionCode(String transactionCode);

    @Query("select t from FinancialTransactionEntity t where t.referenceType = :referenceType and t.referenceId = :referenceId order by t.createdAt desc")
    Page<FinancialTransactionEntity> findByWalletReference(
            @Param("referenceType") String referenceType,
            @Param("referenceId") UUID referenceId,
            Pageable pageable);
}
