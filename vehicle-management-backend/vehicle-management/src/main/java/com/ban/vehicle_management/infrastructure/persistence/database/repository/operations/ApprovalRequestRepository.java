package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ApprovalRequestEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
    interface EmployeeApprovalTimelineProjection {
        UUID getEventId();

        Instant getEventTime();

        ApprovalRequestStatus getStatus();

        String getNote();

        UUID getActorAccountId();

        String getActorUsername();

        String getActorFullName();
    }

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

    @Query("""
            SELECT approval.approvalRequestId AS eventId,
                   COALESCE(approval.approvedAt, approval.createdAt) AS eventTime,
                   approval.status AS status,
                   approval.note AS note,
                   COALESCE(approval.approvedBy, approval.requestedBy) AS actorAccountId,
                   COALESCE(approvedByAccount.username, requestedByAccount.username) AS actorUsername,
                   COALESCE(approvedByProfile.fullName, requestedByProfile.fullName) AS actorFullName
            FROM ApprovalRequestEntity approval
                     LEFT JOIN approval.approvedByAccount approvedByAccount
                     LEFT JOIN approval.requestedByAccount requestedByAccount
                     LEFT JOIN approvedByAccount.userProfile approvedByProfile
                     LEFT JOIN requestedByAccount.userProfile requestedByProfile
            WHERE approval.requestType = :requestType
              AND approval.targetSchema = :targetSchema
              AND approval.targetTable = :targetTable
              AND approval.targetId = :employeeId
            ORDER BY COALESCE(approval.approvedAt, approval.createdAt) DESC
            """)
    List<EmployeeApprovalTimelineProjection> findEmployeeApprovalTimeline(
            @Param("employeeId") UUID employeeId,
            @Param("requestType") String requestType,
            @Param("targetSchema") String targetSchema,
            @Param("targetTable") String targetTable,
            Pageable pageable
    );
}


