package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeSourceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSourceEntity, UUID>, JpaSpecificationExecutor<KnowledgeSourceEntity> {

    boolean existsByTitleIgnoreCase(String title);
}