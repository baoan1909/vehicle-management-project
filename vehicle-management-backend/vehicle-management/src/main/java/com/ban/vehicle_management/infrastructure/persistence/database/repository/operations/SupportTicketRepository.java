package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;

public interface SupportTicketRepository
        extends JpaRepository<SupportTicketEntity, UUID>, JpaSpecificationExecutor<SupportTicketEntity> {

    @Query(value = """
        select distinct ticket.support_ticket_id as "resourceId",
               conversation.organization_id as "organizationId",
               null::uuid as "parkingLotId"
        from operations.support_tickets ticket
        join operations.support_ticket_categories category on category.category_id = ticket.category_id
        join operations.chat_conversations conversation on (
             conversation.conversation_id = ticket.source_conversation_id
             or exists (
                 select 1 from operations.support_ticket_conversation_links link
                 where link.support_ticket_id = ticket.support_ticket_id
                   and link.conversation_id = conversation.conversation_id
                   and link.status = 'ACTIVE'))
        where conversation.organization_id in (:organizationIds)
          and ticket.status in ('OPEN', 'IN_PROGRESS')
          and category.code in ('WRONG_FEE', 'STAFF_COMPLAINT')
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findOrganizationOpenDisputeBlockers(
            @Param("organizationIds") Collection<UUID> organizationIds);

    @Query(value = """
        with assigned_tickets as (
            select ticket.support_ticket_id, ticket.source_conversation_id
              from operations.support_tickets ticket
             where ticket.assigned_to=:accountId and ticket.status in ('OPEN','IN_PROGRESS')
        ), ticket_scopes as (
            select assigned.support_ticket_id, conversation.organization_id
              from assigned_tickets assigned
              join operations.chat_conversations conversation
                on conversation.conversation_id=assigned.source_conversation_id
            union
            select assigned.support_ticket_id, conversation.organization_id
              from assigned_tickets assigned
              join operations.support_ticket_conversation_links link
                on link.support_ticket_id=assigned.support_ticket_id and link.status='ACTIVE'
              join operations.chat_conversations conversation
                on conversation.conversation_id=link.conversation_id
        )
        select assigned.support_ticket_id as "resourceId",
               scope.organization_id as "organizationId",
               null::uuid as "parkingLotId"
          from assigned_tickets assigned
          left join ticket_scopes scope on scope.support_ticket_id=assigned.support_ticket_id
        """, nativeQuery = true)
    List<AccountDeletionResourceScopeProjection> findDeletionBlockingTicketScopes(
            @Param("accountId") UUID accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        with ticket_scopes as (
            select ticket.support_ticket_id, conversation.organization_id
              from operations.support_tickets ticket
              join operations.chat_conversations conversation
                on conversation.conversation_id=ticket.source_conversation_id
            union
            select link.support_ticket_id, conversation.organization_id
              from operations.support_ticket_conversation_links link
              join operations.chat_conversations conversation
                on conversation.conversation_id=link.conversation_id
             where link.status='ACTIVE'
        )
        update operations.support_tickets ticket
           set status='CLOSED', assigned_to=null, closed_at=coalesce(ticket.closed_at, :now),
               close_reason='SYSTEM_MAINTENANCE', auto_close_at=null,
               resolution_note=case
                   when ticket.resolution_note is null or btrim(ticket.resolution_note)='' then 'ORGANIZATION_CLOSED'
                   when position('ORGANIZATION_CLOSED' in ticket.resolution_note)>0 then ticket.resolution_note
                   else ticket.resolution_note || E'\nORGANIZATION_CLOSED'
               end,
               updated_at=:now
         where ticket.status in ('OPEN','IN_PROGRESS')
           and not exists (
               select 1 from operations.support_ticket_categories category
                where category.category_id=ticket.category_id
                  and category.code in ('WRONG_FEE','STAFF_COMPLAINT'))
           and exists (
               select 1 from ticket_scopes scope
                where scope.support_ticket_id=ticket.support_ticket_id
                  and scope.organization_id in (:organizationIds))
           and not exists (
               select 1 from ticket_scopes scope
                where scope.support_ticket_id=ticket.support_ticket_id
                  and (scope.organization_id is null or scope.organization_id not in (:organizationIds)))
        """, nativeQuery = true)
    int closeOrdinaryByOrganizationIds(
            @Param("organizationIds") Collection<UUID> organizationIds,
            @Param("now") java.time.Instant now);

    @Query("""
            select ticket
            from SupportTicketEntity ticket
            join ticket.requesterAccount account
            where ticket.supportTicketId = :supportTicketId
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
            """)
    Optional<SupportTicketEntity> findBusinessVisibleById(@Param("supportTicketId") UUID supportTicketId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ticket
            from SupportTicketEntity ticket
            join ticket.requesterAccount account
            where ticket.supportTicketId = :supportTicketId
              and account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED)
            """)
    Optional<SupportTicketEntity> findBusinessVisibleByIdForUpdate(
            @Param("supportTicketId") UUID supportTicketId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ticket
            from SupportTicketEntity ticket
            where ticket.status = com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus.RESOLVED
              and ticket.autoCloseAt <= :now
            order by ticket.autoCloseAt asc, ticket.supportTicketId asc
            """)
    List<SupportTicketEntity> findDueForAutoClose(@Param("now") Instant now, Pageable pageable);

    boolean existsByCategoryIdAndStatusIn(UUID categoryId, Collection<SupportTicketStatus> statuses);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByRequesterAccountIdAndCategoryIdAndStatusIn(
            UUID requesterAccountId,
            UUID categoryId,
            Collection<SupportTicketStatus> statuses
    );

    Optional<SupportTicketEntity> findFirstByRequesterAccountIdAndCategoryIdAndStatusInOrderByCreatedAtDesc(
            UUID requesterAccountId,
            UUID categoryId,
            List<SupportTicketStatus> statuses
    );

    Optional<SupportTicketEntity> findByRequesterAccountIdAndIdempotencyKey(
            UUID requesterAccountId,
            String idempotencyKey
    );

    @Query(value = "SELECT 1::bigint FROM pg_advisory_xact_lock(hashtextextended(CAST(:requesterAccountId AS text), 0))", nativeQuery = true)
    Long lockRequesterSupport(@Param("requesterAccountId") UUID requesterAccountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE operations.support_tickets
            SET assigned_to = :accountId,
                status = CASE WHEN status = 'OPEN' THEN 'IN_PROGRESS' ELSE status END,
                updated_at = now(),
                updated_by = :accountId
            WHERE support_ticket_id = :supportTicketId
              AND status IN ('OPEN', 'IN_PROGRESS')
              AND (assigned_to IS NULL OR assigned_to = :accountId)
            """, nativeQuery = true)
    int claimIfAvailable(
            @Param("supportTicketId") UUID supportTicketId,
            @Param("accountId") UUID accountId
    );
}
