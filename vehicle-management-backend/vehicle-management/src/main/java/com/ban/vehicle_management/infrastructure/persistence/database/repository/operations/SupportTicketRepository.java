package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
            left join ticket.customer customer
            left join customer.userProfile profile
            left join profile.account account
            where ticket.supportTicketId = :supportTicketId
              and (ticket.customerId is null or account.status not in (
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING_DELETION,
                  com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.DELETED))
            """)
    Optional<SupportTicketEntity> findBusinessVisibleById(@Param("supportTicketId") UUID supportTicketId);

    boolean existsByCategoryIdAndStatusIn(UUID categoryId, Collection<SupportTicketStatus> statuses);

    boolean existsByCustomerIdAndCategoryIdAndStatusIn(
            UUID customerId,
            UUID categoryId,
            Collection<SupportTicketStatus> statuses
    );

    Optional<SupportTicketEntity> findFirstByCustomerIdAndCategoryIdAndStatusInOrderByCreatedAtDesc(
            UUID customerId,
            UUID categoryId,
            List<SupportTicketStatus> statuses
    );

    Optional<SupportTicketEntity> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    @Query(value = "SELECT 1::bigint FROM pg_advisory_xact_lock(hashtextextended(CAST(:customerId AS text), 0))", nativeQuery = true)
    Long lockCustomerSupport(@Param("customerId") UUID customerId);
}
