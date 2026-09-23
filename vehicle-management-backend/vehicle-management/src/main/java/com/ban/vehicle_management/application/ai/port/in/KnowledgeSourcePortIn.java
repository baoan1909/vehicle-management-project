package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.application.ai.command.CreateKnowledgeSourceCommand;
import com.ban.vehicle_management.application.ai.command.UpdateKnowledgeSourceCommand;
import com.ban.vehicle_management.application.ai.query.KnowledgeSourceQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeSourcePortIn {

    List<KnowledgeSource> listSources();

    Page<KnowledgeSource> listSources(KnowledgeSourceQuery query, Pageable pageable);

    KnowledgeSource createSource(CreateKnowledgeSourceCommand command);

    KnowledgeSource updateSource(UUID sourceId, UpdateKnowledgeSourceCommand command);

    void deactivateSource(UUID sourceId);

    void reactivateSource(UUID sourceId);

    List<KnowledgeDocument> listDocuments(UUID sourceId);

    Page<KnowledgeDocument> listDocuments(UUID sourceId, Pageable pageable);
}
