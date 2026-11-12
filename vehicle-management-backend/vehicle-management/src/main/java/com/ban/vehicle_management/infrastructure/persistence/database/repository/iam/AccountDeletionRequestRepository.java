package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionRequestEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequestEntity, UUID> {

    interface DeletedAccountArchiveProjection {
        UUID getAccountId();
        String getAccountStatus();
        UUID getRequestId();
        String getRequestStatus();
        Instant getRequestedAt();
        Instant getScheduledDeletionAt();
        Instant getCompletedAt();
    }

    @Query(value = """
        select account.account_id as "accountId",
               cast(account.status as varchar) as "accountStatus",
               request.request_id as "requestId",
               cast(request.status as varchar) as "requestStatus",
               request.requested_at as "requestedAt",
               request.scheduled_deletion_at as "scheduledDeletionAt",
               request.completed_at as "completedAt"
        from iam.account_deletion_requests request
        join iam.accounts account on account.account_id = request.account_id
        where account.status = 'DELETED'
          and request.status = 'COMPLETED'
        order by request.completed_at desc, request.request_id desc
        """, nativeQuery = true)
    List<DeletedAccountArchiveProjection> findDeletedAccountArchive(Pageable pageable);

    @Query(value = """
        select account.account_id as "accountId",
               cast(account.status as varchar) as "accountStatus",
               request.request_id as "requestId",
               cast(request.status as varchar) as "requestStatus",
               request.requested_at as "requestedAt",
               request.scheduled_deletion_at as "scheduledDeletionAt",
               request.completed_at as "completedAt"
        from iam.account_deletion_requests request
        join iam.accounts account on account.account_id = request.account_id
        where account.account_id = :accountId
          and account.status = 'DELETED'
          and request.status = 'COMPLETED'
        order by request.completed_at desc, request.request_id desc
        limit 1
        """, nativeQuery = true)
    Optional<DeletedAccountArchiveProjection> findDeletedAccountArchiveByAccountId(
            @Param("accountId") UUID accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
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
            """, nativeQuery = true)
    List<UUID> claimDue(
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("claimToken") String claimToken,
            @Param("batchSize") int batchSize);

    Optional<AccountDeletionRequestEntity> findFirstByAccountIdAndStatusInOrderByRequestedAtDesc(
            UUID accountId,
            Collection<AccountDeletionRequestStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select request from AccountDeletionRequestEntity request
        where request.accountId = :accountId and request.status in :statuses
        """)
    Optional<AccountDeletionRequestEntity> findActiveForUpdate(
            @Param("accountId") UUID accountId,
            @Param("statuses") Collection<AccountDeletionRequestStatus> statuses
    );

    Optional<AccountDeletionRequestEntity> findByRequestIdAndClaimToken(UUID requestId, String claimToken);
}
