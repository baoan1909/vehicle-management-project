package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentBlockEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentBlockRepository extends JpaRepository<KnowledgeDocumentBlockEntity, UUID> {

    void deleteByDocumentIdAndDocVersion(UUID documentId, int docVersion);

    List<KnowledgeDocumentBlockEntity> findByDocumentIdAndDocVersionOrderByBlockIndexAsc(
            UUID documentId, int docVersion);

    long countByDocumentId(UUID documentId);
}