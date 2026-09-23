package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiModelCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelCatalogRepository extends JpaRepository<AiModelCatalogEntity, AiModelCatalogEntity.Key> {
}
