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
