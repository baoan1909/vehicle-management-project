package com.ban.vehicle_management.infrastructure.persistence.database.repository.audit;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.AuditLogEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    interface EmployeeAuditTimelineProjection {
        UUID getEventId();

        Instant getEventTime();

        String getAction();

        UUID getActorAccountId();

        String getActorUsername();

        String getActorFullName();
    }

    interface RolePermissionAuditTimelineProjection {
        UUID getEventId();

        Instant getEventTime();

        String getAction();

        UUID getActorAccountId();

        String getActorUsername();

        String getActorFullName();

        Map<String, Object> getOldData();

        Map<String, Object> getNewData();
    }

    @Query("""
            SELECT auditLog.auditLogId AS eventId,
                   auditLog.createdAt AS eventTime,
                   auditLog.action AS action,
                   auditLog.actorAccountId AS actorAccountId,
                   actor.username AS actorUsername,
                   actorProfile.fullName AS actorFullName
            FROM AuditLogEntity auditLog
                     LEFT JOIN auditLog.actorAccount actor
                     LEFT JOIN actor.userProfile actorProfile
            WHERE auditLog.targetSchema = 'people'
              AND auditLog.targetTable = 'employees'
              AND auditLog.targetId = :employeeId
            ORDER BY auditLog.createdAt DESC
            """)
    List<EmployeeAuditTimelineProjection> findEmployeeAuditTimeline(
            @Param("employeeId") UUID employeeId,
            Pageable pageable
    );

    @Query("""
            SELECT auditLog.auditLogId AS eventId,
                   auditLog.createdAt AS eventTime,
                   auditLog.action AS action,
                   auditLog.actorAccountId AS actorAccountId,
                   actor.username AS actorUsername,
                   actorProfile.fullName AS actorFullName,
                   auditLog.oldData AS oldData,
                   auditLog.newData AS newData
            FROM AuditLogEntity auditLog
                     LEFT JOIN auditLog.actorAccount actor
                     LEFT JOIN actor.userProfile actorProfile
            WHERE auditLog.targetSchema = 'iam'
              AND auditLog.targetTable = 'role_permissions'
              AND auditLog.targetId = :roleId
            ORDER BY auditLog.createdAt DESC
            """)
    List<RolePermissionAuditTimelineProjection> findRolePermissionAuditTimeline(
            @Param("roleId") UUID roleId,
            Pageable pageable
    );
}


