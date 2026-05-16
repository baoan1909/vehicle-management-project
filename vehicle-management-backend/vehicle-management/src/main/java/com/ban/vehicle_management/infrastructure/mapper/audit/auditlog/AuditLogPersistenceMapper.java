package com.ban.vehicle_management.infrastructure.mapper.audit.auditlog;

import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.AuditLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogPersistenceMapper {

    AuditLogEntity toEntity(AuditLog domain);

    AuditLog toDomain(AuditLogEntity entity);
}

