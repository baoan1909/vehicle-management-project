package com.ban.vehicle_management.application.operations.approvalrequest.port.out;

import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportTicketEscalationPortOut {

    ApprovalRequest save(ApprovalRequest approvalRequest);

    Optional<ApprovalRequest> findPendingByTicketId(UUID supportTicketId);

    Optional<ApprovalRequest> findByRequesterAndIdempotencyKey(UUID requestedBy, String idempotencyKey);

    Optional<ApprovalRequest> findByIdForUpdate(UUID escalationId);

    List<ApprovalRequest> findAll(ApprovalRequestStatus status);

    long countRecentByRequester(UUID requestedBy, Instant since);
}
