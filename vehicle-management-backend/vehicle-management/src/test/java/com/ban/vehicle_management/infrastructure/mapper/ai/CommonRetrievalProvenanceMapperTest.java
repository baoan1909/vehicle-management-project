package com.ban.vehicle_management.infrastructure.mapper.ai;

import static org.junit.jupiter.api.Assertions.*;
import com.ban.vehicle_management.domain.ai.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CommonRetrievalProvenanceMapperTest {
    @Test void commonProvenanceSurvivesMappingWithoutInventingProviderIndexOrModelRun() {
        var mapper=Mappers.getMapper(RetrievalAuditPersistenceMapper.class);
        var audit=new RetrievalAudit(UUID.randomUUID(),null,null,null,null,null,null,"q","hash",null,
                "reviewed-faq-v1/canonical-question-v1","threshold", "PUBLIC",0,1,1,1,5,2,
                BigDecimal.ONE,null,2L,Instant.now(),List.of(),"COMMON_FAQ","document@2:content-sha");
        var entity=mapper.toEntity(audit);assertEquals("COMMON_FAQ",entity.getRetrievalMethod());
        assertEquals(audit.sourceSnapshot(),entity.getSourceSnapshot());assertNull(entity.getActiveIndexVersionId());assertNull(entity.getRunId());
        var restored=mapper.toDomain(entity,List.of());assertEquals(audit.sourceSnapshot(),restored.sourceSnapshot());
        assertEquals("COMMON_FAQ",restored.retrievalMethod());assertNull(restored.activeIndexVersionId());assertNull(restored.runId());
    }
}
