package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock;
import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentBlockPortOut {

    void replaceForVersion(UUID documentId, int documentVersion, List<KnowledgeDocumentBlock> blocks);

    List<KnowledgeDocumentBlock> findByDocumentIdAndDocVersion(UUID documentId, int documentVersion);

    long countByDocumentId(UUID documentId);
}