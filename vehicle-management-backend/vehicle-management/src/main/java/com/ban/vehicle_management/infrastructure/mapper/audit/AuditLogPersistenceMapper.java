package com.ban.vehicle_management.infrastructure.mapper.audit;

import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionAuditLogResult;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.AuditLogEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.audit.AuditLogRepository;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogPersistenceMapper {

    AuditLogEntity toEntity(AuditLog domain);

    AuditLog toDomain(AuditLogEntity entity);

    RolePermissionAuditLogResult toRolePermissionAuditLogResult(
            AuditLogRepository.RolePermissionAuditTimelineProjection projection
    );

    List<RolePermissionAuditLogResult> toRolePermissionAuditLogResults(
            List<AuditLogRepository.RolePermissionAuditTimelineProjection> projections
    );
}


