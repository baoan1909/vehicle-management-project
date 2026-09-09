package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SupportTicketEscalationPortOut;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.infrastructure.mapper.operations.ApprovalRequestPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketEscalationPersistenceAdapter implements SupportTicketEscalationPortOut {

    public static final String REQUEST_TYPE = "SUPPORT_TICKET_ESCALATION";
    public static final String TARGET_SCHEMA = "operations";
    public static final String TARGET_TABLE = "support_tickets";

    private final ApprovalRequestRepository repository;
    private final ApprovalRequestPersistenceMapper mapper;

    public SupportTicketEscalationPersistenceAdapter(
            ApprovalRequestRepository repository,
            ApprovalRequestPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ApprovalRequest save(ApprovalRequest approvalRequest) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(approvalRequest)));
    }

    @Override
    public Optional<ApprovalRequest> findPendingByTicketId(UUID supportTicketId) {
        return repository.findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdAndStatusOrderByCreatedAtDesc(
                REQUEST_TYPE, TARGET_SCHEMA, TARGET_TABLE, supportTicketId, ApprovalRequestStatus.PENDING
        ).map(mapper::toDomain);
    }

    @Override
    public Optional<ApprovalRequest> findByRequesterAndIdempotencyKey(UUID requestedBy, String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return repository.findByRequestedByAndRequestTypeAndIdempotencyKey(
                requestedBy, REQUEST_TYPE, idempotencyKey
        ).map(mapper::toDomain);
    }

    @Override
    public Optional<ApprovalRequest> findByIdForUpdate(UUID escalationId) {
        return repository.findSupportEscalationForUpdate(
                escalationId, REQUEST_TYPE, TARGET_SCHEMA, TARGET_TABLE
        ).map(mapper::toDomain);
    }

    @Override
    public List<ApprovalRequest> findAll(ApprovalRequestStatus status) {
        List<com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity> rows =
                status == null
                        ? repository.findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
                                REQUEST_TYPE, TARGET_SCHEMA, TARGET_TABLE
                        )
                        : repository.findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
                                REQUEST_TYPE, TARGET_SCHEMA, TARGET_TABLE, status
                        );
        return rows.stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countRecentByRequester(UUID requestedBy, Instant since) {
        return repository.countByRequestTypeAndRequestedByAndCreatedAtGreaterThanEqual(
                REQUEST_TYPE, requestedBy, since
        );
    }
}
