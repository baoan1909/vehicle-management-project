package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountStatusHistoryEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDeletionLifecycleRepository {
    private final EntityManager entityManager;

    public AccountDeletionLifecycleRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<AccountEntity> findAccount(UUID accountId) {
        return Optional.ofNullable(entityManager.find(AccountEntity.class, accountId));
    }

    public Optional<AccountEntity> findAccountForUpdate(UUID accountId) {
        return Optional.ofNullable(entityManager.find(AccountEntity.class, accountId, LockModeType.PESSIMISTIC_WRITE));
    }

    public void appendStatusHistory(
            UUID accountId,
            AccountStatus oldStatus,
            AccountStatus newStatus,
            String reason,
            Instant changedAt
    ) {
        entityManager.persist(new AccountStatusHistoryEntity(
                UUID.randomUUID(), accountId, null, oldStatus, newStatus, reason, changedAt, null, null));
    }

    @SuppressWarnings("unchecked")
    public List<UUID> claimDue(Instant now, Instant leaseUntil, String claimToken, int batchSize) {
        return entityManager.createNativeQuery("""
                update iam.account_deletion_requests request
                   set status = 'PROCESSING', claim_token = :claimToken,
                       processing_started_at = :now, processing_lease_until = :leaseUntil,
                       attempt_count = attempt_count + 1, updated_at = :now, version = version + 1
                 where request.request_id in (
                       select candidate.request_id from iam.account_deletion_requests candidate
                        where ((candidate.status in ('GRACE_PERIOD','RETRY')
                                and candidate.scheduled_deletion_at <= :now
                                and (candidate.next_retry_at is null or candidate.next_retry_at <= :now))
                               or (candidate.status = 'PROCESSING' and candidate.processing_lease_until <= :now))
                        order by candidate.scheduled_deletion_at, candidate.request_id
                        for update skip locked limit :batchSize)
                returning request.request_id
                """).setParameter("claimToken", claimToken)
                .setParameter("now", now)
                .setParameter("leaseUntil", leaseUntil)
                .setParameter("batchSize", batchSize)
                .getResultList();
    }

    public void flush() {
        entityManager.flush();
    }
}
