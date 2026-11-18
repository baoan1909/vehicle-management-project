package com.ban.vehicle_management.application.operations.supportticket.port.out;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportTicketPortOut {
    SupportTicket save(SupportTicket supportTicket);
    Optional<SupportTicket> findById(UUID supportTicketId);
    Optional<SupportTicket> findByIdForUpdate(UUID supportTicketId);
    List<SupportTicket> findDueForAutoClose(Instant now, int limit);
    Optional<SupportTicket> findByRequesterAccountIdAndIdempotencyKey(UUID requesterAccountId, String idempotencyKey);
    void lockRequesterSupport(UUID requesterAccountId);

    List<SupportTicket> findAll(
            UUID requesterAccountId,
            String requesterRoleCode,
            UUID categoryId,
            UUID assignedTo,
            UUID organizationId,
            UUID parkingLotId,
            String featureCode,
            SupportTicketStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    );

    List<SupportTicket> findOwnTickets(
            UUID requesterAccountId,
            SupportTicketStatus status,
            String keyword,
            int limit
    );

    boolean existsActiveCategoryById(UUID categoryId);
    boolean existsAssignableAccountById(UUID accountId);
    boolean existsActiveWorkflowByRequesterAccountIdAndCategoryId(UUID requesterAccountId, UUID categoryId);
    Optional<SupportTicket> findActiveWorkflowByRequesterAccountIdAndCategoryId(UUID requesterAccountId, UUID categoryId);
    boolean claimIfAvailable(UUID supportTicketId, UUID accountId);
}
