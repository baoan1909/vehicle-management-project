package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIngestionJobEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeIngestionJobEventRepository
        extends JpaRepository<KnowledgeIngestionJobEventEntity, Long> {

    long countByIngestionJobId(UUID ingestionJobId);

    List<KnowledgeIngestionJobEventEntity> findByIngestionJobIdOrderByCreatedAtAsc(UUID ingestionJobId);
}