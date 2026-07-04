package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
    Optional<ApprovalRequestEntity> findByApprovalRequestIdAndRequestTypeAndTargetSchemaAndTargetTable(
            UUID approvalRequestId,
            String requestType,
            String targetSchema,
            String targetTable
    );

    Optional<ApprovalRequestEntity> findTopByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdOrderByCreatedAtDesc(
            String requestType,
            String targetSchema,
            String targetTable,
            UUID targetId
    );

    boolean existsByRequestTypeAndTargetSchemaAndTargetTableAndTargetIdAndStatus(
            String requestType,
            String targetSchema,
            String targetTable,
            UUID targetId,
            ApprovalRequestStatus status
    );

    List<ApprovalRequestEntity> findByRequestTypeAndTargetSchemaAndTargetTableOrderByCreatedAtDesc(
            String requestType,
            String targetSchema,
            String targetTable
    );

    List<ApprovalRequestEntity> findByRequestTypeAndTargetSchemaAndTargetTableAndStatusOrderByCreatedAtDesc(
            String requestType,
            String targetSchema,
            String targetTable,
            ApprovalRequestStatus status
    );
}


