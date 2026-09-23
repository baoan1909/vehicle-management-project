package com.ban.vehicle_management.infrastructure.mapper.ai;

import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.RetrievalAuditEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.RetrievalAuditResultEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RetrievalAuditPersistenceMapper {

    @Mapping(target = "resultCount", ignore = true)
    RetrievalAuditEntity toEntity(RetrievalAudit domain);

    @Mapping(target = "auditResultId", ignore = true)
    @Mapping(target = "retrievalAuditId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RetrievalAuditResultEntity toResultEntity(RetrievalAudit.RetrievalAuditResult domain);

    RetrievalAudit toDomain(RetrievalAuditEntity entity, java.util.List<RetrievalAudit.RetrievalAuditResult> results);

    RetrievalAudit.RetrievalAuditResult toResultDomain(RetrievalAuditResultEntity entity);
}
