package com.ban.vehicle_management.infrastructure.persistence.adapter.audit;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.rolepermission.model.result.RolePermissionAuditLogResult;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.infrastructure.mapper.audit.AuditLogPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.audit.AuditLogEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.audit.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AuditLogPersistenceAdapter implements AuditLogPortOut {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogPersistenceMapper auditLogPersistenceMapper;

    public AuditLogPersistenceAdapter(
            AuditLogRepository auditLogRepository,
            AuditLogPersistenceMapper auditLogPersistenceMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogPersistenceMapper = auditLogPersistenceMapper;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity savedEntity = auditLogRepository.save(auditLogPersistenceMapper.toEntity(auditLog));
        return auditLogPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public List<RolePermissionAuditLogResult> findRolePermissionAuditLogs(UUID roleId, int limit) {
        return auditLogPersistenceMapper.toRolePermissionAuditLogResults(
                auditLogRepository.findRolePermissionAuditTimeline(roleId, PageRequest.of(0, limit))
        );
    }
}
