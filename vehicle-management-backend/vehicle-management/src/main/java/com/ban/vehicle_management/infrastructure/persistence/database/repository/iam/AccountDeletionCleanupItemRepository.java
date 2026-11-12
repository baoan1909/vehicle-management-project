package com.ban.vehicle_management.infrastructure.persistence.database.repository.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionCleanupItemEntity;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupResourceType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountDeletionCleanupItemRepository
        extends JpaRepository<AccountDeletionCleanupItemEntity, UUID> {

    List<AccountDeletionCleanupItemEntity> findAllByRequestIdOrderByScheduledAtAscCleanupItemIdAsc(
            UUID requestId);

    @Modifying
    @Query(value = """
        insert into iam.account_deletion_cleanup_items(
            cleanup_item_id, request_id, organization_id, resource_type,
            external_identifier, scheduled_at, status, attempt_count,
            version, created_at)
        values (gen_random_uuid(), :requestId, :organizationId, :resourceType,
                :externalIdentifier, :scheduledAt, 'SCHEDULED', 0, 0, now())
        on conflict (request_id, resource_type, external_identifier) do nothing
        """, nativeQuery = true)
    void scheduleIfAbsent(
            @Param("requestId") UUID requestId,
            @Param("organizationId") UUID organizationId,
            @Param("resourceType") String resourceType,
            @Param("externalIdentifier") String externalIdentifier,
            @Param("scheduledAt") Instant scheduledAt);

    Optional<AccountDeletionCleanupItemEntity> findByCleanupItemIdAndClaimToken(
            UUID cleanupItemId, String claimToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update iam.account_deletion_cleanup_items item
           set status = 'PROCESSING', claim_token = :claimToken,
               processing_lease_until = :leaseUntil,
               attempt_count = attempt_count + 1, updated_at = :now,
               version = version + 1
         where item.cleanup_item_id in (
               select candidate.cleanup_item_id
                 from iam.account_deletion_cleanup_items candidate
                where candidate.request_id = :requestId
                  and candidate.scheduled_at <= :now
                  and ((candidate.status in ('SCHEDULED','RETRY')
                        and (candidate.next_retry_at is null or candidate.next_retry_at <= :now))
                       or (candidate.status = 'PROCESSING'
                           and candidate.processing_lease_until <= :now))
                order by candidate.scheduled_at, candidate.cleanup_item_id
                for update skip locked limit :batchSize)
        returning item.cleanup_item_id
        """, nativeQuery = true)
    List<UUID> claimDueForRequest(
            @Param("requestId") UUID requestId,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("claimToken") String claimToken,
            @Param("batchSize") int batchSize);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update iam.account_deletion_cleanup_items item
           set status = 'PROCESSING', claim_token = :claimToken,
               processing_lease_until = :leaseUntil,
               attempt_count = attempt_count + 1, updated_at = :now,
               version = version + 1
         where item.cleanup_item_id in (
               select candidate.cleanup_item_id
                 from iam.account_deletion_cleanup_items candidate
                 join iam.account_deletion_requests request
                   on request.request_id = candidate.request_id
                where request.status = 'COMPLETED'
                  and candidate.scheduled_at <= :now
                  and ((candidate.status in ('SCHEDULED','RETRY')
                        and (candidate.next_retry_at is null or candidate.next_retry_at <= :now))
                       or (candidate.status = 'PROCESSING'
                           and candidate.processing_lease_until <= :now))
                order by candidate.scheduled_at, candidate.cleanup_item_id
                for update skip locked limit :batchSize)
        returning item.cleanup_item_id
        """, nativeQuery = true)
    List<UUID> claimDueForCompletedRequests(
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("claimToken") String claimToken,
            @Param("batchSize") int batchSize);

    boolean existsByRequestIdAndScheduledAtLessThanEqualAndStatusNot(
            UUID requestId, Instant scheduledAt,
            com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionCleanupStatus status);
}
