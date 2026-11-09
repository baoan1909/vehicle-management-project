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

public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequestEntity, UUID> {

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
