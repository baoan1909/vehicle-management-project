package com.ban.vehicle_management.infrastructure.mapper.audit.auditlog;

import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.infrastructure.persistence.audit.auditlog.AuditLogEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class AuditLogPersistenceMapperImpl implements AuditLogPersistenceMapper {

    @Override
    public AuditLogEntity toEntity(AuditLog domain) {
        if ( domain == null ) {
            return null;
        }

        AuditLogEntity auditLogEntity = new AuditLogEntity();

        auditLogEntity.setCreatedAt( domain.getCreatedAt() );
        auditLogEntity.setCreatedBy( domain.getCreatedBy() );
        auditLogEntity.setUpdatedAt( domain.getUpdatedAt() );
        auditLogEntity.setUpdatedBy( domain.getUpdatedBy() );
        auditLogEntity.setAuditLogId( domain.getAuditLogId() );
        auditLogEntity.setActorAccountId( domain.getActorAccountId() );
        auditLogEntity.setAction( domain.getAction() );
        auditLogEntity.setTargetSchema( domain.getTargetSchema() );
        auditLogEntity.setTargetTable( domain.getTargetTable() );
        auditLogEntity.setTargetId( domain.getTargetId() );
        Map<String, Object> map = domain.getOldData();
        if ( map != null ) {
            auditLogEntity.setOldData( new LinkedHashMap<String, Object>( map ) );
        }
        Map<String, Object> map1 = domain.getNewData();
        if ( map1 != null ) {
            auditLogEntity.setNewData( new LinkedHashMap<String, Object>( map1 ) );
        }
        auditLogEntity.setIpAddress( domain.getIpAddress() );
        auditLogEntity.setUserAgent( domain.getUserAgent() );

        return auditLogEntity;
    }

    @Override
    public AuditLog toDomain(AuditLogEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AuditLog auditLog = new AuditLog();

        auditLog.setCreatedAt( entity.getCreatedAt() );
        auditLog.setCreatedBy( entity.getCreatedBy() );
        auditLog.setUpdatedAt( entity.getUpdatedAt() );
        auditLog.setUpdatedBy( entity.getUpdatedBy() );
        auditLog.setAuditLogId( entity.getAuditLogId() );
        auditLog.setActorAccountId( entity.getActorAccountId() );
        auditLog.setAction( entity.getAction() );
        auditLog.setTargetSchema( entity.getTargetSchema() );
        auditLog.setTargetTable( entity.getTargetTable() );
        auditLog.setTargetId( entity.getTargetId() );
        Map<String, Object> map = entity.getOldData();
        if ( map != null ) {
            auditLog.setOldData( new LinkedHashMap<String, Object>( map ) );
        }
        Map<String, Object> map1 = entity.getNewData();
        if ( map1 != null ) {
            auditLog.setNewData( new LinkedHashMap<String, Object>( map1 ) );
        }
        auditLog.setIpAddress( entity.getIpAddress() );
        auditLog.setUserAgent( entity.getUserAgent() );

        return auditLog;
    }
}
