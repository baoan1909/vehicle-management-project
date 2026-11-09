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
import org.springframework.data.repository.query.Param;

public interface SupportTicketRepository
        extends JpaRepository<SupportTicketEntity, UUID>, JpaSpecificationExecutor<SupportTicketEntity> {

    @Query(value = """
        select support_ticket_id from operations.support_tickets
        where assigned_to=:accountId and status in ('OPEN','IN_PROGRESS')
        """, nativeQuery = true)
    List<UUID> findDeletionBlockingTicketIds(@Param("accountId") UUID accountId);

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
